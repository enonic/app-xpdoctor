package me.myklebust.xpdoctor.validator.nodevalidator.blobmissing;

import java.util.List;
import java.util.Map;

import org.elasticsearch.action.get.GetResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import me.myklebust.xpdoctor.storagespy.StorageSpyService;
import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.ValidatorResult;
import me.myklebust.xpdoctor.validator.ValidatorResultImpl;
import me.myklebust.xpdoctor.validator.ValidatorResults;
import me.myklebust.xpdoctor.validator.nodevalidator.AbstractNodeExecutor;
import me.myklebust.xpdoctor.validator.nodevalidator.BatchedQueryExecutor;

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
    extends AbstractNodeExecutor
{
    public static final SegmentLevel NODE_SEGMENT_LEVEL = SegmentLevel.from( "node" );

    public static final SegmentLevel INDEX_CONFIG_SEGMENT_LEVEL = SegmentLevel.from( "index" );

    public static final SegmentLevel ACCESS_CONTROL_SEGMENT_LEVEL = SegmentLevel.from( "access" );

    public static final SegmentLevel BINARY_SEGMENT_LEVEL = SegmentLevel.from( "binary" );

    private final static String TYPE = "Missing Blob";

    public static final int BATCH_SIZE = 1_000;

    private final NodeService nodeService;

    private final StorageSpyService storageSpyService;

    private final BlobStore blobStore;

    private final BlobMissingDoctor doctor;


    private Logger LOG = LoggerFactory.getLogger( BlobMissingExecutor.class );

    private BlobMissingExecutor( final Builder builder )
    {
        super( builder );
        this.nodeService = builder.nodeService;
        this.storageSpyService = builder.storageSpyService;
        this.blobStore = builder.blobStore;
        this.doctor = builder.doctor;
    }


    public static Builder create()
    {
        return new Builder();
    }

    public ValidatorResults execute()
    {
        LOG.info( "Running LoadableNodeExecutor..." );

        reportStart();

        final BatchedQueryExecutor executor = BatchedQueryExecutor.create().batchSize( BATCH_SIZE ).nodeService( this.nodeService ).build();

        final ValidatorResults.Builder results = ValidatorResults.create();

        int execute = 0;

        while ( executor.hasMore() )
        {
            LOG.info( "Checking nodes " + execute + "->" + ( execute + BATCH_SIZE ) + " of " + executor.getTotalHits() );
            reportProgress( executor.getTotalHits(), execute );

            final NodeIds nodesToCheck = executor.execute();
            results.add( checkNodes( nodesToCheck, false ) );
            execute += BATCH_SIZE;
        }

        LOG.info( ".... LoadableNodeExecutor done" );

        return results.build();
    }

    private List<ValidatorResult> checkNodes( final NodeIds nodeIds, final boolean repair )
    {
        List<ValidatorResult> results = Lists.newArrayList();

        for ( final NodeId nodeId : nodeIds )
        {
            doCheckNode( repair, results, nodeId );
        }
        return results;
    }

    private void doCheckNode( final boolean repair, final List<ValidatorResult> results, final NodeId nodeId )
    {
        try
        {
            final GetResponse response =
                storageSpyService.getInBranch( nodeId, ContextAccessor.current().getRepositoryId(), ContextAccessor.current().getBranch() );
            final Map<String, Object> sourceAsMap = response.getSourceAsMap();
            final BlobKey nodeBlobKey = BlobKey.from( ((List<String>) sourceAsMap.get( "nodeblobkey" )).get( 0 ) );
            final BlobKey accessBlobKey = BlobKey.from( ((List<String>) sourceAsMap.get( "accesscontrolblobkey" )).get( 0 ) );
            final BlobKey indexBlobKey = BlobKey.from( ((List<String>) sourceAsMap.get( "indexconfigblobkey" )).get( 0 ) );
            final NodeVersionId versionid = NodeVersionId.from( ((List<String>) sourceAsMap.get( "versionid" )).get( 0 ) );

            final BlobRecord nodeBlobRecord =
                blobStore.getRecord( RepositorySegmentUtils.toSegment( ContextAccessor.current().getRepositoryId(), NODE_SEGMENT_LEVEL ),
                                     nodeBlobKey );
            if (nodeBlobRecord == null) {
                resolveAndRepaid( repair, results, nodeId, new Exception( "Node blob is missing" ) );
            }

            final BlobRecord accessBlobRecord =
                blobStore.getRecord( RepositorySegmentUtils.toSegment( ContextAccessor.current().getRepositoryId(), ACCESS_CONTROL_SEGMENT_LEVEL ),
                                     accessBlobKey );
            if (accessBlobRecord == null) {
                resolveAndRepaid( repair, results, nodeId, new Exception( "Access control blob is missing" ) );
            }

            final BlobRecord indexBlobRecord =
                blobStore.getRecord( RepositorySegmentUtils.toSegment( ContextAccessor.current().getRepositoryId(), INDEX_CONFIG_SEGMENT_LEVEL ),
                                     indexBlobKey );
            if (indexBlobRecord == null) {
                resolveAndRepaid( repair, results, nodeId, new Exception( "Index config blob is missing" ) );
            }

            final GetResponse versionResponse = storageSpyService.getVersion( nodeId, versionid, ContextAccessor.current().getRepositoryId() );
            final List<String> binaryblobkeys = (List<String>) versionResponse.getSourceAsMap().get( "binaryblobkeys" );
            if (binaryblobkeys != null) {
                for ( String binaryblobkey : binaryblobkeys )
                {
                    final BlobRecord binaryBlobRecord = blobStore.getRecord(
                        RepositorySegmentUtils.toSegment( ContextAccessor.current().getRepositoryId(), BINARY_SEGMENT_LEVEL ),
                        BlobKey.from( binaryblobkey ) );
                    if (binaryBlobRecord == null) {
                        resolveAndRepaid( repair, results, nodeId, new Exception( "Binary blob is missing " + binaryblobkey ) );
                    } else {
                        System.out.println( "Binary blob found" );
                    }
                };
            }

        }
        catch ( Exception e )
        {
            resolveAndRepaid( repair, results, nodeId, e );
        }
    }

    private void resolveAndRepaid( final boolean doRepair, final List<ValidatorResult> results, final NodeId nodeId, final Exception e )
    {
        try
        {
            final RepairResult repairResult = this.doctor.repairBlob( nodeId, doRepair );

            final ValidatorResultImpl result = ValidatorResultImpl.create()
                .nodeId( nodeId )
                .nodePath( null )
                .nodeVersionId( null )
                .timestamp( null )
                .type( TYPE )
                .validatorName( validatorName )
                .message( e.getMessage() )
                .repairResult( repairResult )
                .build();

            results.add( result );
        }
        catch ( Exception e1 )
        {
            LOG.error( "Failed to repair", e1 );
        }
    }

    public static final class Builder
        extends AbstractNodeExecutor.Builder<Builder>
    {
        private NodeService nodeService;

        private BlobMissingDoctor doctor;

        private StorageSpyService storageSpyService;

        private BlobStore blobStore;

        private Builder()
        {
        }

        public Builder nodeService( final NodeService val )
        {
            nodeService = val;
            return this;
        }

        public Builder doctor( final BlobMissingDoctor val )
        {
            doctor = val;
            return this;
        }

        public Builder storageSpyService( final StorageSpyService storageSpyService )
        {
            this.storageSpyService = storageSpyService;
            return this;
        }

        public BlobMissingExecutor build()
        {
            return new BlobMissingExecutor( this );
        }

        public Builder blobStore( final BlobStore blobStore )
        {
            this.blobStore = blobStore;
            return this;
        }
    }
}
