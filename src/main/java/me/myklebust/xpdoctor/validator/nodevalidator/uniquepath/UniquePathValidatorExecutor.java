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
import me.myklebust.xpdoctor.validator.StorageSpyService;
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
import com.enonic.xp.node.NodeQuery;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.node.NodeVersionMetadata;
import com.enonic.xp.repository.Repository;
import com.enonic.xp.repository.RepositoryService;

import static me.myklebust.xpdoctor.validator.nodevalidator.uniquepath.UniquePathDoctor.PREFIX;

public class UniquePathValidatorExecutor
{
    private static final Logger LOG = LoggerFactory.getLogger( UniquePathValidatorExecutor.class );

    private final NodeService nodeService;

    private final RepositoryService repositoryService;

    private final StorageSpyService storageSpyService;

    private final NonUniquePathsHolder nonUniquePathsHolder = new NonUniquePathsHolder();

    private final List<ValidatorResult.Builder> tmpResults = new ArrayList<>();

    public UniquePathValidatorExecutor( final NodeService nodeService, final RepositoryService repositoryService, StorageSpyService storageSpyService )
    {
        this.nodeService = nodeService;
        this.repositoryService = repositoryService;
        this.storageSpyService = storageSpyService;
    }

    public void execute( final Reporter reporter)
    {
        LOG.info( "Running UniquePathValidatorExecutor..." );
        reporter.reportStart();

        BatchedQueryExecutor.create().spyStorageService( this.storageSpyService ).
            progressReporter( reporter.getProgressReporter() ).
            build().execute( nodesToCheck -> checkNodes( nodesToCheck, reporter ) );

        LOG.info( "... UniquePathValidatorExecutor done" );
    }

    private void checkNodes( final NodeIds nodeIds, final Reporter reporter )
    {
        for ( final NodeId nodeId : nodeIds )
        {
            try
            {
                final Node node = this.nodeService.getById( nodeId );

                checkNode( node );
            }
            catch ( Exception e )
            {
                LOG.error( "Cannot check unique path for node with id: {}", nodeId, e );
            }
        }

        for ( ValidatorResult.Builder tmpResult : tmpResults )
        {
            final boolean childHasTrouble = this.nonUniquePathsHolder.myChildHasAProblem( tmpResult.nodePath() );
            tmpResult.repairResult( RepairResult.create()
                                        .message( childHasTrouble
                                                      ? "child must be repaired first"
                                                      : "rename to " + tmpResult.nodePath().getName() + PREFIX )
                                        .repairStatus( childHasTrouble ? RepairStatus.DEPENDENT_ON_OTHER : RepairStatus.IS_REPAIRABLE )
                                        .build() ).validatorName( reporter.validatorName );
            reporter.addResult( tmpResult );
        }
    }

    private void checkNode( final Node node )
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
            createNonUniqueEntry( node, queryResult );
        }
    }

    private void createNonUniqueEntry( final Node node, final FindNodesByQueryResult queryResult )
    {
        final ValidatorResult.Builder result = ValidatorResult.create().
            nodeId( node.id() ).
            nodePath( node.path() ).
            nodeVersionId( node.getNodeVersionId() ).
            timestamp( node.getTimestamp() ).
            type( "Non-unique path" )
            .message( queryResult.getNodeIds().stream().map( this::addNotUniqueEntry ).collect( Collectors.joining( ";" ) ) );

        this.nonUniquePathsHolder.add( node.path() );

        tmpResults.add( result );
    }

    private String addNotUniqueEntry( final NodeId nodeId )
    {
        final Repository repository = this.repositoryService.get( ContextAccessor.current().getRepositoryId() );

        final GetActiveNodeVersionsResult activeVersions = this.nodeService.getActiveVersions(
            GetActiveNodeVersionsParams.create().nodeId( nodeId ).branches( repository.getBranches() ).build() );

        final ImmutableMap<Branch, NodeVersionMetadata> nodeVersions = activeVersions.getNodeVersions();

        final Set<NodeId> ids = nodeVersions.values().stream().
            map( NodeVersionMetadata::getNodeId ).
            collect( Collectors.toSet() );

        return String.format( "id: [%s]", Joiner.on( "," ).join( ids ) );
    }
}
