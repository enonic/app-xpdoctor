package me.myklebust.xpdoctor.validator.nodevalidator.binaryblobmissing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.StorageSpyService;
import me.myklebust.xpdoctor.validator.ValidatorResult;
import me.myklebust.xpdoctor.validator.nodevalidator.Reporter;
import me.myklebust.xpdoctor.validator.nodevalidator.ScrollQueryExecutor;

import com.enonic.xp.blob.BlobStore;
import com.enonic.xp.blob.SegmentLevel;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeIds;

public class BlobMissingExecutor
{
    private static final Logger LOG = LoggerFactory.getLogger( BlobMissingExecutor.class );

    public static final SegmentLevel NODE_SEGMENT_LEVEL = SegmentLevel.from( "node" );

    public static final SegmentLevel INDEX_CONFIG_SEGMENT_LEVEL = SegmentLevel.from( "index" );

    public static final SegmentLevel ACCESS_CONTROL_SEGMENT_LEVEL = SegmentLevel.from( "access" );

    public static final SegmentLevel BINARY_SEGMENT_LEVEL = SegmentLevel.from( "binary" );

    private final static String TYPE = "Missing Blobs";

    private final StorageSpyService storageSpyService;

    private final BlobStore blobStore;

    private final BlobMissingDoctor doctor;

    public BlobMissingExecutor( final StorageSpyService storageSpyService, final BlobStore blobStore,
                                final BlobMissingDoctor doctor )
    {
        this.storageSpyService = storageSpyService;
        this.blobStore = blobStore;
        this.doctor = doctor;
    }

    public void execute( final Reporter reporter )
    {
        LOG.info( "Running BlobMissingExecutor..." );

        reporter.reportStart();

        ScrollQueryExecutor.create()
            .progressReporter( reporter.getProgressReporter() )
            .indexType( ScrollQueryExecutor.IndexType.STORAGE )
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
            final MissingBlobsService.MissingBlobsResult missingBlobsResult =
                new MissingBlobsService( blobStore, storageSpyService ).checkMissingBlobs( nodeId );

            if ( !missingBlobsResult.isOk() )
            {
                final String message =
                    ( !missingBlobsResult.binaryblobkeys.isEmpty() ? "Binary blobs missing (" + missingBlobsResult.binaryblobkeys.size() +
                        "): " + missingBlobsResult.binaryblobkeys : "" ) + " " +
                        ( missingBlobsResult.nodeblobkey != null ? "nodeblob missing: " + missingBlobsResult.nodeblobkey : "" ) + " " +
                        ( missingBlobsResult.accesscontrolblobkey != null ? "accesscontrolblob missing " +
                            missingBlobsResult.accesscontrolblobkey : "" ) + " " + ( missingBlobsResult.indexconfigblobkey != null
                        ? "indexconfigblob missing " + missingBlobsResult.indexconfigblobkey
                        : "" );
                resolveAndRepair( results, nodeId, message );
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
                                   .message( message )
                                   .repairResult( repairResult ) );
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to repair", e );
        }
    }
}
