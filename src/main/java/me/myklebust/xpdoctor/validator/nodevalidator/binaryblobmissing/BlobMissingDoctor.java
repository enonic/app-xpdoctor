package me.myklebust.xpdoctor.validator.nodevalidator.binaryblobmissing;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.enonic.xp.repository.Repository;
import com.enonic.xp.repository.RepositoryId;
import com.enonic.xp.repository.RepositorySegmentUtils;
import com.enonic.xp.repository.RepositoryService;

import static me.myklebust.xpdoctor.validator.nodevalidator.binaryblobmissing.BlobMissingExecutor.ACCESS_CONTROL_SEGMENT_LEVEL;
import static me.myklebust.xpdoctor.validator.nodevalidator.binaryblobmissing.BlobMissingExecutor.BINARY_SEGMENT_LEVEL;
import static me.myklebust.xpdoctor.validator.nodevalidator.binaryblobmissing.BlobMissingExecutor.INDEX_CONFIG_SEGMENT_LEVEL;
import static me.myklebust.xpdoctor.validator.nodevalidator.binaryblobmissing.BlobMissingExecutor.NODE_SEGMENT_LEVEL;

public class BlobMissingDoctor
    implements NodeDoctor
{
    private final BlobStore blobStore;

    private final RepositoryService repositoryService;

    private final StorageSpyService storageSpyService;

    public BlobMissingDoctor( final BlobStore blobStore, final RepositoryService repositoryService,
                              final StorageSpyService storageSpyService )
    {
        this.blobStore = blobStore;
        this.repositoryService = repositoryService;
        this.storageSpyService = storageSpyService;
    }

    @Override
    public RepairResult repairNode( final NodeId nodeId, final boolean dryRun )
    {
        final MissingBlobsService.MissingBlobsResult
            binaryBlobKeysToRestore = new MissingBlobsService( blobStore, storageSpyService).checkMissingBlobs( nodeId );
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
            final BlobRecord matchingBlobRecord = findMatchingBlobRecord( BINARY_SEGMENT_LEVEL, blobKey );
            if ( matchingBlobRecord != null )
            {
                binaryBlobRecords.add( matchingBlobRecord );
            }
        }

        final RepositoryId currentRepositoryId = ContextAccessor.current().getRepositoryId();
        final Segment binarySegment = RepositorySegmentUtils.toSegment( currentRepositoryId, BINARY_SEGMENT_LEVEL );
        final Segment nodeSegment = RepositorySegmentUtils.toSegment( currentRepositoryId, NODE_SEGMENT_LEVEL );
        final Segment accessSegment =
            RepositorySegmentUtils.toSegment( currentRepositoryId, ACCESS_CONTROL_SEGMENT_LEVEL );
        final Segment indexSegment =
            RepositorySegmentUtils.toSegment( currentRepositoryId, INDEX_CONFIG_SEGMENT_LEVEL );

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

        if ( !binaryBlobRecords.isEmpty() || accesscontrolblobRecord != null || indexconfigblobRecord != null ||
            nodeblobRecord != null )
        {
            final Set<BlobKey> binaryBlobsRestored = binaryBlobRecords.stream().map( BlobRecord::getKey ).collect( Collectors.toSet() );
            final String message = ( !binaryBlobRecords.isEmpty() ? "binary blobs " + ( dryRun ? "have been" : "can be" ) + " restored (" +
                binaryBlobsRestored.size() + "): " + binaryBlobsRestored : "" ) + " " + ( nodeblobRecord != null
                ? "nodeblob " + ( dryRun ? "has been" : "can be" ) + " restored: " + nodeblobRecord.getKey()
                : "" ) + " " +
                ( accesscontrolblobRecord != null ? "accesscontrolblob " + ( dryRun ? "has been" : "can be" ) + " restored: " +
                    accesscontrolblobRecord.getKey() : "" ) + " " +
                ( indexconfigblobRecord != null ? "indexconfigblob " + ( dryRun ? "has been" : "can be" ) + " restored: " +
                    indexconfigblobRecord.getKey() : "" );

            return RepairResult.create()
                .message( message )
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
