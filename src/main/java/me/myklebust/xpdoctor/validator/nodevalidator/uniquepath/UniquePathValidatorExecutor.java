package me.myklebust.xpdoctor.validator.nodevalidator.uniquepath;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.ValidatorResult;
import me.myklebust.xpdoctor.validator.nodevalidator.BatchedQueryExecutor;
import me.myklebust.xpdoctor.validator.nodevalidator.Reporter;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.node.FindNodesByQueryResult;
import com.enonic.xp.node.GetActiveNodeVersionsParams;
import com.enonic.xp.node.GetActiveNodeVersionsResult;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeIds;
import com.enonic.xp.node.NodeIndexPath;
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
{
    private final NodeService nodeService;

    private final RepositoryService repositoryService;

    private NonUniquePathsHolder nonUniquePathsHolder = new NonUniquePathsHolder();

    private final Logger LOG = LoggerFactory.getLogger( UniquePathValidatorExecutor.class );

    public UniquePathValidatorExecutor( final NodeService nodeService, final RepositoryService repositoryService )
    {
        this.nodeService = nodeService;
        this.repositoryService = repositoryService;
    }

    public void execute( final Reporter reporter)
    {
        LOG.info( "Running UniquePathValidatorExecutor..." );
        reporter.reportStart();

        final BatchedQueryExecutor executor = BatchedQueryExecutor.create().
            nodeService( this.nodeService ).
            progressReporter( reporter.getProgressReporter() ).
            orderBy( OrderExpressions.from( FieldOrderExpr.create( NodeIndexPath.PATH, OrderExpr.Direction.DESC ) ) ).
            build();

        while ( executor.hasMore() )
        {
            executor.nextBatch(nodesToCheck -> checkNodes( nodesToCheck, reporter ) );
        }
    }

    private List<ValidatorResult> checkNodes( final NodeIds nodeIds, final Reporter reporter )
    {
        List<ValidatorResult> results = new ArrayList<>();

        for ( final NodeId nodeId : nodeIds )
        {
            try
            {
                final Node node = this.nodeService.getById( nodeId );

                checkNode( node, reporter );

            }
            catch ( Exception e )
            {
                LOG.error( "Cannot check unique path for node with id: " + nodeId + "", e );
            }
        }
        return results;
    }

    private void checkNode( final Node node, final Reporter reporter )
    {
        if ( nonUniquePathsHolder.has( node.path() ) )
        {
            return;
        }

        final FindNodesByQueryResult queryResult = this.nodeService.findByQuery( NodeQuery.create().
            path( node.path() ).
            size( -1 ).
            build() );

        final boolean pathIsNotUnique = queryResult.getTotalHits() > 1;

        if ( pathIsNotUnique )
        {
            createNonUniqueEntry( node, queryResult, reporter );
        }
    }

    private void createNonUniqueEntry( final Node node, final FindNodesByQueryResult queryResult, final Reporter reporter )
    {
        final ValidatorResult.Builder result = ValidatorResult.create().
            nodeId( node.id() ).
            nodePath( node.path() ).
            nodeVersionId( node.getNodeVersionId() ).
            timestamp( node.getTimestamp() ).
            validatorName( reporter.validatorName ).
            type( "Non-unique path" );

        final ArrayList<String> messages = new ArrayList<>();

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

        reporter.addResult( result.build() );
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
}
