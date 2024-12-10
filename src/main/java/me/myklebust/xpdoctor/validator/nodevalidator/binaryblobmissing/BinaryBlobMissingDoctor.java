package me.myklebust.xpdoctor.validator.nodevalidator.binaryblobmissing;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.get.GetResponse;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.StorageSpyService;
import me.myklebust.xpdoctor.validator.nodevalidator.NodeDoctor;

import com.enonic.xp.blob.BlobKey;
import com.enonic.xp.blob.BlobRecord;
import com.enonic.xp.blob.BlobStore;
import com.enonic.xp.blob.Segment;
import com.enonic.xp.blob.SegmentLevel;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeVersionId;
import com.enonic.xp.repository.Repository;
import com.enonic.xp.repository.RepositorySegmentUtils;
import com.enonic.xp.repository.RepositoryService;

import static me.myklebust.xpdoctor.validator.nodevalidator.binaryblobmissing.BinaryBlobMissingExecutor.ACCESS_CONTROL_SEGMENT_LEVEL;
import static me.myklebust.xpdoctor.validator.nodevalidator.binaryblobmissing.BinaryBlobMissingExecutor.BINARY_SEGMENT_LEVEL;
import static me.myklebust.xpdoctor.validator.nodevalidator.binaryblobmissing.BinaryBlobMissingExecutor.INDEX_CONFIG_SEGMENT_LEVEL;
import static me.myklebust.xpdoctor.validator.nodevalidator.binaryblobmissing.BinaryBlobMissingExecutor.NODE_SEGMENT_LEVEL;

public class BinaryBlobMissingDoctor
    implements NodeDoctor
{
    private final BlobStore blobStore;

    private final RepositoryService repositoryService;

    private final StorageSpyService storageSpyService;

    public BinaryBlobMissingDoctor( final BlobStore blobStore, final RepositoryService repositoryService,
                                    final StorageSpyService storageSpyService )
    {
        this.blobStore = blobStore;
        this.repositoryService = repositoryService;
        this.storageSpyService = storageSpyService;
    }

    @Override
    public RepairResult repairNode( final NodeId nodeId, final boolean dryRun )
    {
        final MissingBlobsService.BlobRefs binaryBlobKeysToRestore = new MissingBlobsService(blobStore, storageSpyService).missingBlobs( nodeId );
        if ( binaryBlobKeysToRestore.isOk() )
        {
            return RepairResult.create().repairStatus( RepairStatus.NOT_NEEDED ).message( "Blobs are OK already" ).build();
        }

        final BlobRecord accesscontrolblobRecord =
            findMatchingBlobRecord( BINARY_SEGMENT_LEVEL, binaryBlobKeysToRestore.accesscontrolblobkey );
        final BlobRecord indexconfigblobRecord =
            findMatchingBlobRecord( INDEX_CONFIG_SEGMENT_LEVEL, binaryBlobKeysToRestore.indexconfigblobkey );
        final BlobRecord nodeblobRecord = findMatchingBlobRecord( NODE_SEGMENT_LEVEL, binaryBlobKeysToRestore.nodeblobkey );

        final List<BlobRecord> binaryBlobRecords = new ArrayList<>();
        for ( BlobKey blobKey : binaryBlobKeysToRestore.binaryblobkeys )
        {
            for ( Repository repository : repositoryService.list() )
            {
                final BlobRecord record =
                    blobStore.getRecord( RepositorySegmentUtils.toSegment( repository.getId(), BINARY_SEGMENT_LEVEL ), blobKey );
                if ( record != null )
                {
                    binaryBlobRecords.add( record );
                    break;
                }
            }
        }

        final Segment binarySegment = RepositorySegmentUtils.toSegment( ContextAccessor.current().getRepositoryId(), BINARY_SEGMENT_LEVEL );
        final Segment nodeSegment = RepositorySegmentUtils.toSegment( ContextAccessor.current().getRepositoryId(), NODE_SEGMENT_LEVEL );
        final Segment accessSegment =
            RepositorySegmentUtils.toSegment( ContextAccessor.current().getRepositoryId(), ACCESS_CONTROL_SEGMENT_LEVEL );
        final Segment indexSegment =
            RepositorySegmentUtils.toSegment( ContextAccessor.current().getRepositoryId(), INDEX_CONFIG_SEGMENT_LEVEL );

        if ( !dryRun )
        {
            if ( accesscontrolblobRecord != null )
            {
                blobStore.addRecord( accessSegment, accesscontrolblobRecord );
            }
            if ( indexconfigblobRecord != null )
            {
                blobStore.addRecord( indexSegment, indexconfigblobRecord );
            }
            if ( nodeblobRecord != null )
            {
                blobStore.addRecord( nodeSegment, nodeblobRecord );
            }
            for ( BlobRecord blobRecord : binaryBlobRecords )
            {
                blobStore.addRecord( binarySegment, blobRecord );
            }
        }

        if ( !binaryBlobKeysToRestore.binaryblobkeys.isEmpty() || accesscontrolblobRecord != null || indexconfigblobRecord != null ||
            nodeblobRecord != null )
        {
            return RepairResult.create()
                .message( dryRun ? "Some (maybe all)  Blobs can be restored" : "Blobs restored" )
                .repairStatus( dryRun ? RepairStatus.IS_REPAIRABLE : RepairStatus.REPAIRED )
                .build();
        }
        else
        {
            return RepairResult.create().message( "No blobs found to restore from" ).repairStatus( RepairStatus.NOT_REPAIRABLE ).build();
        }
    }

    private BlobRecord findMatchingBlobRecord( SegmentLevel segmentLevel, BlobKey blobKey )
    {
        if ( blobKey != null )
        {
            for ( Repository repository : repositoryService.list() )
            {
                final BlobRecord record =
                    blobStore.getRecord( RepositorySegmentUtils.toSegment( repository.getId(), segmentLevel ), blobKey );
                if ( record != null )
                {
                    return record;
                }
            }
        }
        return null;
    }
}
