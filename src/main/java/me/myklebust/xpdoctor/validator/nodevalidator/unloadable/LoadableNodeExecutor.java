package me.myklebust.xpdoctor.validator.nodevalidator.unloadable;

import java.util.List;

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

import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeIds;
import com.enonic.xp.node.NodeService;

public class LoadableNodeExecutor
    extends AbstractNodeExecutor
{
    private final static String TYPE = "Unloadable node";

    public static final int BATCH_SIZE = 1_000;

    private final NodeService nodeService;

    private final StorageSpyService storageSpyService;

    private final LoadableNodeDoctor doctor;

    private final UnloadableNodeReasonResolver reasonResolver;

    private Logger LOG = LoggerFactory.getLogger( LoadableNodeExecutor.class );

    private LoadableNodeExecutor( final Builder builder )
    {
        super( builder );
        this.nodeService = builder.nodeService;
        this.storageSpyService = builder.storageSpyService;
        this.doctor = builder.doctor;
        this.reasonResolver = new UnloadableNodeReasonResolver( storageSpyService );
    }


    public static Builder create()
    {
        return new Builder();
    }

    public ValidatorResults execute()
    {
        LOG.info( "Running LoadableNodeExecutor..." );

        reportStart();

        final BatchedQueryExecutor executor = BatchedQueryExecutor.create().
            batchSize( BATCH_SIZE ).
            nodeService( this.nodeService ).
            build();

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
            Node foundNodeId = this.nodeService.getById( nodeId );

            if ( foundNodeId == null )
            {
                LOG.info( "##### NODE %s IS NULL BUT NO EXCEPTION!?", nodeId );
            }
        }
        catch ( Exception e )
        {
            resolveAndRepaid( repair, results, nodeId, e );
        }
    }

    private void resolveAndRepaid( final boolean doRepair, final List<ValidatorResult> results, final NodeId nodeId, final Exception e )
    {
        UnloadableReason reason;

        try
        {
            reason = reasonResolver.resolve( nodeId );
        }
        catch ( Exception e1 )
        {
            LOG.error( "Not able to resolve reason for unloadable node", e );
            return;
        }

        try
        {

            final RepairResult repairResult = this.doctor.repairNode( nodeId, doRepair, reason );

            final ValidatorResultImpl result = ValidatorResultImpl.create().
                nodeId( nodeId ).
                nodePath( null ).
                nodeVersionId( null ).
                timestamp( null ).
                type( TYPE ).
                validatorName( validatorName ).
                message( e.getMessage() ).
                repairResult( repairResult ).
                build();

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

        private LoadableNodeDoctor doctor;

        private StorageSpyService storageSpyService;

        private Builder()
        {
        }

        public Builder nodeService( final NodeService val )
        {
            nodeService = val;
            return this;
        }

        public Builder doctor( final LoadableNodeDoctor val )
        {
            doctor = val;
            return this;
        }

        public Builder storageSpyService( final StorageSpyService storageSpyService )
        {
            this.storageSpyService = storageSpyService;
            return this;
        }

        public LoadableNodeExecutor build()
        {
            return new LoadableNodeExecutor( this );
        }
    }
}
