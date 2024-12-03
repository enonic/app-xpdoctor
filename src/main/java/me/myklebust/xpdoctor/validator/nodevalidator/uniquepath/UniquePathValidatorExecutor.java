package me.myklebust.xpdoctor.validator.nodevalidator.uniquepath;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.ValidatorResult;
import me.myklebust.xpdoctor.validator.ValidatorResultImpl;
import me.myklebust.xpdoctor.validator.ValidatorResults;
import me.myklebust.xpdoctor.validator.nodevalidator.AbstractNodeExecutor;
import me.myklebust.xpdoctor.validator.nodevalidator.BatchedQueryExecutor;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.node.FindNodesByQueryResult;
import com.enonic.xp.node.GetActiveNodeVersionsParams;
import com.enonic.xp.node.GetActiveNodeVersionsResult;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeIds;
import com.enonic.xp.node.NodeIndexPath;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.node.NodeQuery;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.node.NodeVersionMetadata;
import com.enonic.xp.query.expr.FieldOrderExpr;
import com.enonic.xp.query.expr.OrderExpr;
import com.enonic.xp.query.expr.OrderExpressions;
import com.enonic.xp.repository.Repository;
import com.enonic.xp.repository.RepositoryService;

import static me.myklebust.xpdoctor.validator.nodevalidator.uniquepath.UniquePathDoctor.PREFIX;

public class UniquePathValidatorExecutor
    extends AbstractNodeExecutor
{
    public static final int BATCH_SIZE = 1_000;

    private final NodeService nodeService;

    private final RepositoryService repositoryService;

    private NonUniquePathsHolder nonUniquePathsHolder = new NonUniquePathsHolder();

    private final Set<NodePath> handledPaths = Sets.newHashSet();

    private final Logger LOG = LoggerFactory.getLogger( UniquePathValidatorExecutor.class );

    private UniquePathValidatorExecutor( final Builder builder )
    {
        super( builder );
        nodeService = builder.nodeService;
        repositoryService = builder.repositoryService;
    }

    public static Builder create()
    {
        return new Builder();
    }

    public ValidatorResults execute()
    {
        LOG.info( "Running UniquePathValidatorExecutor..." );
        reportStart();

        final BatchedQueryExecutor executor = BatchedQueryExecutor.create().
            batchSize( BATCH_SIZE ).
            nodeService( this.nodeService ).
            orderBy( OrderExpressions.from( FieldOrderExpr.create( NodeIndexPath.PATH, OrderExpr.Direction.DESC ) ) ).
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
                final Node node = this.nodeService.getById( nodeId );

                final ValidatorResult result = checkNode( node );

                if ( result != null )
                {
                    results.add( result );
                }
            }
            catch ( Exception e )
            {
                LOG.error( "Cannot check unique path for node with id: " + nodeId + "", e );
            }
        }
        return results;
    }

    private ValidatorResult checkNode( final Node node )
    {
        if ( nonUniquePathsHolder.has( node.path() ) )
        {
            return null;
        }

        final FindNodesByQueryResult queryResult = this.nodeService.findByQuery( NodeQuery.create().
            path( node.path() ).
            size( -1 ).
            build() );

        final boolean pathIsNotUnique = queryResult.getTotalHits() > 1;

        if ( pathIsNotUnique )
        {
            return createNonUniqueEntry( node, queryResult );
        }

        return null;
    }

    private ValidatorResult createNonUniqueEntry( final Node node, final FindNodesByQueryResult queryResult )
    {
        final ValidatorResultImpl.Builder result = ValidatorResultImpl.create().
            nodeId( node.id() ).
            nodePath( node.path() ).
            nodeVersionId( node.getNodeVersionId() ).
            timestamp( node.getTimestamp() ).
            validatorName( this.validatorName ).
            type( "Non-unique path" );

        final ArrayList<String> messages = Lists.newArrayList();

        for ( final NodeId nodeId : queryResult.getNodeIds() )
        {
            messages.add( addNotUniqueEntry( nodeId ) );
        }

        System.out.println( "Duplicate entries found: " + Joiner.on( ";" ).join( messages ) );

        result.message( Joiner.on( ";" ).join( messages ) );

        this.nonUniquePathsHolder.add( node.path() );

        System.out.println( "Adding to nonUniquePaths: " + node.path() );

        final boolean childHasTrouble = this.nonUniquePathsHolder.myChildHasAProblem( node.path() );
        result.repairResult( RepairResult.create().
            message( childHasTrouble ? "child must be repaired first" : "rename to " + node.name() + PREFIX ).
            repairStatus( childHasTrouble ? RepairStatus.DEPENDENT_ON_OTHER : RepairStatus.IS_REPAIRABLE ).
            build() );

        return result.build();
    }

    private String addNotUniqueEntry( final NodeId nodeId )
    {
        final Repository repository = this.repositoryService.get( ContextAccessor.current().getRepositoryId() );

        final GetActiveNodeVersionsResult activeVersions = getBranches( nodeId, repository );

        final ImmutableMap<Branch, NodeVersionMetadata> nodeVersions = activeVersions.getNodeVersions();

        final Set<NodeId> ids = nodeVersions.values().stream().
            map( NodeVersionMetadata::getNodeId ).
            collect( Collectors.toSet() );

        return String.format( "id: [%s]", Joiner.on( "," ).join( ids ) );
    }

    private GetActiveNodeVersionsResult getBranches( final NodeId nodeId, final Repository repository )
    {
        return this.nodeService.getActiveVersions( GetActiveNodeVersionsParams.create().
            nodeId( nodeId ).
            branches( repository.getBranches() ).
            build() );
    }

    public static final class Builder
        extends AbstractNodeExecutor.Builder<Builder>
    {
        private NodeService nodeService;

        private RepositoryService repositoryService;

        private Builder()
        {
        }

        public Builder nodeService( final NodeService val )
        {
            nodeService = val;
            return this;
        }

        public Builder repositoryService( final RepositoryService val )
        {
            repositoryService = val;
            return this;
        }

        public UniquePathValidatorExecutor build()
        {
            return new UniquePathValidatorExecutor( this );
        }
    }
}
