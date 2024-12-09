package me.myklebust.xpdoctor.validator.nodevalidator.blobmissing;

import java.util.List;
import java.util.Map;

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
import com.enonic.xp.node.NodeService;
import com.enonic.xp.node.NodeVersionId;
import com.enonic.xp.repository.RepositorySegmentUtils;

public class BlobMissingExecutor
{
    private static final Logger LOG = LoggerFactory.getLogger( BlobMissingExecutor.class );

    private static final SegmentLevel BINARY_SEGMENT_LEVEL = SegmentLevel.from( "binary" );

    private final static String TYPE = "Missing Blob";

    private final NodeService nodeService;

    private final StorageSpyService storageSpyService;

    private final BlobStore blobStore;

    private final BlobMissingDoctor doctor;

    public BlobMissingExecutor( final NodeService nodeService, final StorageSpyService storageSpyService, final BlobStore blobStore,
                                final BlobMissingDoctor doctor )
    {
        this.nodeService = nodeService;
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
            .nodeService( this.nodeService )
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
            final GetResponse response =
                storageSpyService.getInBranch( nodeId, ContextAccessor.current().getRepositoryId(), ContextAccessor.current().getBranch() );
            final Map<String, Object> sourceAsMap = response.getSourceAsMap();
            final NodeVersionId versionid = NodeVersionId.from( ( (List<String>) sourceAsMap.get( "versionid" ) ).get( 0 ) );

            final GetResponse versionResponse =
                storageSpyService.getVersion( nodeId, versionid, ContextAccessor.current().getRepositoryId() );
            final List<String> binaryblobkeys = (List<String>) versionResponse.getSourceAsMap().get( "binaryblobkeys" );
            if ( binaryblobkeys != null )
            {
                for ( String binaryblobkey : binaryblobkeys )
                {
                    final BlobRecord binaryBlobRecord = blobStore.getRecord(
                        RepositorySegmentUtils.toSegment( ContextAccessor.current().getRepositoryId(), BINARY_SEGMENT_LEVEL ),
                        BlobKey.from( binaryblobkey ) );
                    if ( binaryBlobRecord == null )
                    {
                        resolveAndRepair( results, nodeId, "Binary blob is missing " + binaryblobkey );
                    }
                }
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
