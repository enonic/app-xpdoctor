package me.myklebust.xpdoctor.validator.nodevalidator.binaryblobmissing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.StorageSpyService;
import me.myklebust.xpdoctor.validator.ValidatorResult;
import me.myklebust.xpdoctor.validator.nodevalidator.Reporter;
import me.myklebust.xpdoctor.validator.nodevalidator.ScrollQueryExecutor;

import com.enonic.xp.blob.BlobStore;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeIds;

public class BlobCorruptedExecutor
{
    private static final Logger LOG = LoggerFactory.getLogger( BlobCorruptedExecutor.class );

    private final static String TYPE = "Corrupted Blobs";

    private final StorageSpyService storageSpyService;

    private final BlobStore blobStore;

    private final BlobCorruptedDoctor doctor;

    public BlobCorruptedExecutor( final StorageSpyService storageSpyService, final BlobStore blobStore,
                                  final BlobCorruptedDoctor doctor )
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
            final CorruptedBlobsService.MissingBlobsResult missingBlobsResult =
                new CorruptedBlobsService( blobStore, storageSpyService ).checkMissingBlobs( nodeId );

            if ( !missingBlobsResult.isOk() )
            {
                final String message = missingBlobsResult.toString();
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
