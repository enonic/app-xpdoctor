package me.myklebust.xpdoctor.validator.nodevalidator.binaryblobmissing;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.get.GetResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.myklebust.xpdoctor.validator.StorageSpyService;
import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.ValidatorResult;
import me.myklebust.xpdoctor.validator.nodevalidator.BatchedQueryExecutor;
import me.myklebust.xpdoctor.validator.nodevalidator.Reporter;

import com.enonic.xp.blob.BlobKey;
import com.enonic.xp.blob.BlobRecord;
import com.enonic.xp.blob.BlobStore;
import com.enonic.xp.blob.SegmentLevel;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeIds;
import com.enonic.xp.node.NodeVersionId;
import com.enonic.xp.repository.RepositorySegmentUtils;

public class BinaryBlobMissingExecutor
{
    private static final Logger LOG = LoggerFactory.getLogger( BinaryBlobMissingExecutor.class );

    public static final SegmentLevel NODE_SEGMENT_LEVEL = SegmentLevel.from( "node" );

    public static final SegmentLevel INDEX_CONFIG_SEGMENT_LEVEL = SegmentLevel.from( "index" );

    public static final SegmentLevel ACCESS_CONTROL_SEGMENT_LEVEL = SegmentLevel.from( "access" );

    public static final SegmentLevel BINARY_SEGMENT_LEVEL = SegmentLevel.from( "binary" );

    private final static String TYPE = "Missing Blob";

    private final StorageSpyService storageSpyService;

    private final BlobStore blobStore;

    private final BinaryBlobMissingDoctor doctor;

    public BinaryBlobMissingExecutor( final StorageSpyService storageSpyService, final BlobStore blobStore,
                                      final BinaryBlobMissingDoctor doctor )
    {
        this.storageSpyService = storageSpyService;
        this.blobStore = blobStore;
        this.doctor = doctor;
    }

    public void execute( final Reporter reporter )
    {
        LOG.info( "Running BlobMissingExecutor..." );

        reporter.reportStart();

        BatchedQueryExecutor.create()
            .progressReporter( reporter.getProgressReporter() )
            .spyStorageService( this.storageSpyService )
            .build()
            .execute( nodesToCheck -> checkNodes( nodesToCheck, reporter ) );

        LOG.info( ".... BlobMissingExecutor done" );
    }

    private void checkNodes( final NodeIds nodeIds, final Reporter results )
    {

        for ( final NodeId nodeId : nodeIds )
        {
            doCheckNode( results, nodeId );
        }
    }

    private void doCheckNode( final Reporter results, final NodeId nodeId )
    {
        try
        {
            final MissingBlobsService.BlobRefs binaryBlobKeysToRestore = new MissingBlobsService(blobStore, storageSpyService).missingBlobs( nodeId );

            if (!binaryBlobKeysToRestore.isOk()) {
                resolveAndRepair( results, nodeId, "Binary blobs missing: " + binaryBlobKeysToRestore.binaryblobkeys + " " +
                    ( binaryBlobKeysToRestore.nodeblobkey == null ? "nodeblob missing" : "" ) +
                    ( binaryBlobKeysToRestore.accesscontrolblobkey == null ? "accesscontrolblob missing" : "" ) +
                    ( binaryBlobKeysToRestore.indexconfigblobkey == null ? "indexconfigblob missing" : "" ) );
            }
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to check", e );
        }
    }

    private void resolveAndRepair( final Reporter results, final NodeId nodeId, final String message )
    {
        try
        {
            final RepairResult repairResult = this.doctor.repairNode( nodeId, true );

            results.addResult( ValidatorResult.create()
                                   .nodeId( nodeId )
                                   .nodePath( null )
                                   .nodeVersionId( null )
                                   .timestamp( null )
                                   .type( TYPE )
                                   .validatorName( results.validatorName )
                                   .message( message )
                                   .repairResult( repairResult )
                                   .build() );
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to repair", e );
        }
    }
}
