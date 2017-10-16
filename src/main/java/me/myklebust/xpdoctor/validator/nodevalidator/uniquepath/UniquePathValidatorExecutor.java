package me.myklebust.xpdoctor.validator.nodevalidator.uniquepath;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import me.myklebust.xpdoctor.validator.RepairResultImpl;
import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.ValidatorResult;
import me.myklebust.xpdoctor.validator.ValidatorResultImpl;
import me.myklebust.xpdoctor.validator.ValidatorResults;
import me.myklebust.xpdoctor.validator.nodevalidator.AbstractNodeExecutor;
import me.myklebust.xpdoctor.validator.nodevalidator.BatchedQueryExecutor;

import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.node.FindNodesByQueryResult;
import com.enonic.xp.node.GetActiveNodeVersionsParams;
import com.enonic.xp.node.GetActiveNodeVersionsResult;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeIds;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.node.NodeQuery;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.repository.Repository;
import com.enonic.xp.repository.RepositoryService;
import com.enonic.xp.task.ProgressReporter;

public class UniquePathValidatorExecutor
    extends AbstractNodeExecutor
{
    public static final int BATCH_SIZE = 1_000;

    private final NodeService nodeService;

    private final RepositoryService repositoryService;

    private final Set<NodePath> handledPaths = Sets.newHashSet();

    private final Logger LOG = LoggerFactory.getLogger( UniquePathValidatorExecutor.class );

    public UniquePathValidatorExecutor( final NodeService nodeService, final RepositoryService repositoryService,
                                        final ProgressReporter reporter )
    {
        super( reporter );
        this.nodeService = nodeService;
        this.repositoryService = repositoryService;
    }

    public ValidatorResults execute()
    {
        LOG.info( "Running UniquePathValidatorExecutor..." );
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
        if ( handledPaths.contains( node.path() ) )
        {
            return null;
        }

        final FindNodesByQueryResult queryResult = this.nodeService.findByQuery( NodeQuery.create().
            path( node.path() ).
            size( -1 ).
            build() );

        this.handledPaths.add( node.path() );

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
            type( "Non-unique path" );

        final ArrayList<String> messages = Lists.newArrayList();

        for ( final NodeId nodeId : queryResult.getNodeIds() )
        {
            messages.add( addNotUniqueEntry( nodeId ) );
        }

        System.out.println( "Messages: " + Joiner.on( ";" ).join( messages ) );

        result.message( Joiner.on( ";" ).join( messages ) );

        result.repairResult( RepairResultImpl.create().message( "not started" ).repairStatus( RepairStatus.UNKNOW ).build() );

        return result.build();
    }

    private String addNotUniqueEntry( final NodeId nodeId )
    {
        final Node foundNode = this.nodeService.getById( nodeId );

        final Repository repository = this.repositoryService.get( ContextAccessor.current().getRepositoryId() );

        final GetActiveNodeVersionsResult activeVersions = getBranches( nodeId, repository );

        return String.format( "Path: [%s], Branches: [%s]", foundNode.path(),
                              Joiner.on( "," ).join( activeVersions.getNodeVersions().keySet() ) );
    }

    private GetActiveNodeVersionsResult getBranches( final NodeId nodeId, final Repository repository )
    {
        return this.nodeService.getActiveVersions( GetActiveNodeVersionsParams.create().
            nodeId( nodeId ).
            branches( repository.getBranches() ).
            build() );
    }
}
