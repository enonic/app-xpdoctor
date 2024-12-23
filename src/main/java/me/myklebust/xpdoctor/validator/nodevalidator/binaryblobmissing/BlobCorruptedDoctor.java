package me.myklebust.xpdoctor.validator.nodevalidator.binaryblobmissing;

import java.util.ArrayList;
import java.util.List;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.StorageSpyService;
import me.myklebust.xpdoctor.validator.nodevalidator.NodeDoctor;

import com.enonic.xp.blob.BlobKey;
import com.enonic.xp.blob.BlobRecord;
import com.enonic.xp.blob.BlobStore;
import com.enonic.xp.blob.SegmentLevel;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.repository.Repository;
import com.enonic.xp.repository.RepositorySegmentUtils;
import com.enonic.xp.repository.RepositoryService;

public class BlobCorruptedDoctor
    implements NodeDoctor
{
    public static final SegmentLevel NODE_SEGMENT_LEVEL = SegmentLevel.from( "node" );

    public static final SegmentLevel INDEX_CONFIG_SEGMENT_LEVEL = SegmentLevel.from( "index" );

    public static final SegmentLevel ACCESS_CONTROL_SEGMENT_LEVEL = SegmentLevel.from( "access" );

    public static final SegmentLevel BINARY_SEGMENT_LEVEL = SegmentLevel.from( "binary" );

    private final BlobStore blobStore;

    private final RepositoryService repositoryService;

    private final StorageSpyService storageSpyService;

    public BlobCorruptedDoctor( final BlobStore blobStore, final RepositoryService repositoryService,
                                final StorageSpyService storageSpyService )
    {
        this.blobStore = blobStore;
        this.repositoryService = repositoryService;
        this.storageSpyService = storageSpyService;
    }

    @Override
    public RepairResult repairNode( final NodeId nodeId, final boolean dryRun )
    {
        final CorruptedBlobsService.MissingBlobsResult
            binaryBlobKeysToRestore = new CorruptedBlobsService( blobStore, storageSpyService).checkMissingBlobs( nodeId );
        if ( binaryBlobKeysToRestore.isOk() )
        {
            return RepairResult.create().repairStatus( RepairStatus.NOT_NEEDED ).message( "Blobs are OK already" ).build();
        }

        List<BlobRepairReport> repairReport = new ArrayList<>();
        binaryBlobKeysToRestore.blobReports.forEach( ( blobReport ) -> {
            if ( blobReport.state == BlobReport.BlobState.MISSING )
            {
                final BlobRecord blobRecord = findMatchingBlobRecord( blobReport.segmentLevel, blobReport.blobKey );
                if ( blobRecord != null )
                {
                    if ( !dryRun )
                    {
                        blobStore.addRecord(
                            RepositorySegmentUtils.toSegment( ContextAccessor.current().getRepositoryId(), blobReport.segmentLevel ),
                            blobRecord );
                    }
                    repairReport.add( new BlobRepairReport( blobReport.segmentLevel, blobReport.blobKey,
                                                            dryRun ? RepairStatus.REPAIRED : RepairStatus.IS_REPAIRABLE ) );
                }
            }
            else
            {
                repairReport.add( new BlobRepairReport( blobReport.segmentLevel, blobReport.blobKey, RepairStatus.NOT_REPAIRABLE ) );
            }
        } );

        if ( repairReport.stream().allMatch( blobRepairReport -> blobRepairReport.repairStatus == RepairStatus.NOT_REPAIRABLE ) )
        {
            return RepairResult.create().message( "No blobs found to restore from" ).repairStatus( RepairStatus.NOT_REPAIRABLE ).build();
        }
        else
        {
            return RepairResult.create()
                .message( repairReport.toString() )
                .repairStatus( dryRun ? RepairStatus.IS_REPAIRABLE : RepairStatus.REPAIRED )
                .build();
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

    public static class BlobRepairReport
    {
        SegmentLevel segmentLevel;
        BlobKey blobKey;
        RepairStatus repairStatus;

        public BlobRepairReport( final SegmentLevel segmentLevel, final BlobKey blobKey, final RepairStatus repairStatus )
        {
            this.segmentLevel = segmentLevel;
            this.blobKey = blobKey;
            this.repairStatus = repairStatus;
        }

        @Override
        public String toString()
        {
            return segmentLevel + ": " + blobKey + "[] -> " + repairStatus;
        }
    }
}
