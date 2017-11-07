package me.myklebust.xpdoctor.validator.nodevalidator.parentexists;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import me.myklebust.xpdoctor.validator.RepairResultImpl;
import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.ValidatorResult;
import me.myklebust.xpdoctor.validator.ValidatorResultImpl;
import me.myklebust.xpdoctor.validator.ValidatorResults;
import me.myklebust.xpdoctor.validator.nodevalidator.AbstractNodeExecutor;
import me.myklebust.xpdoctor.validator.nodevalidator.BatchedQueryExecutor;

import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeIds;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.node.NodeService;

public class ParentExistsExistsExecutor
    extends AbstractNodeExecutor
{
    public static final int BATCH_SIZE = 1_000;

    private final NodeService nodeService;

    private final Logger LOG = LoggerFactory.getLogger( ParentExistsExistsExecutor.class );

    private ParentExistsExistsExecutor( final Builder builder )
    {
        super( builder );
        nodeService = builder.nodeService;
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

            results.add( checkNodes( executor.execute() ) );
            execute += BATCH_SIZE;
        }

        return results.build();
    }

    private List<ValidatorResult> checkNodes( final NodeIds nodeIds )
    {
        List<ValidatorResult> results = Lists.newArrayList();

        for ( final NodeId nodeId : nodeIds )
        {
            try
            {
                final ValidatorResult result = doCheckNode( results, nodeId );

                if ( result != null )
                {
                    results.add( result );
                }

            }
            catch ( Exception e )
            {
                LOG.error( "Cannot check parent exists for node with id: " + nodeId + "", e );
            }
        }
        return results;
    }

    private ValidatorResult doCheckNode( final List<ValidatorResult> results, final NodeId nodeId )
    {
        final Node node = this.nodeService.getById( nodeId );

        final NodePath parentPath = node.path().getParentPath();

        final Node parent = this.nodeService.getByPath( parentPath );

        if ( parent == null )
        {
            return ValidatorResultImpl.create().
                nodeId( nodeId ).
                nodePath( node.path() ).
                nodeVersionId( node.getNodeVersionId() ).
                timestamp( node.getTimestamp() ).
                type( "No parent" ).
                validatorName( validatorName ).
                message( "Parent with path : " + node.parentPath() + " not found" ).
                repairResult( RepairResultImpl.create().
                    message( "Create parent with path [" + node.parentPath() + "]" ).
                    repairStatus( RepairStatus.MANUAL ).
                    build() ).
                build();
        }

        return null;
    }

    public static final class Builder
        extends AbstractNodeExecutor.Builder<Builder>
    {
        private NodeService nodeService;

        private Builder()
        {
        }

        public Builder nodeService( final NodeService val )
        {
            nodeService = val;
            return this;
        }

        public ParentExistsExistsExecutor build()
        {
            return new ParentExistsExistsExecutor( this );
        }
    }
}
