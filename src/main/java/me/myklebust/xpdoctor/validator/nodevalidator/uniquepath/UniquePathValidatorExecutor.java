package me.myklebust.xpdoctor.validator.nodevalidator.uniquepath;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import me.myklebust.xpdoctor.validator.ValidatorResult;
import me.myklebust.xpdoctor.validator.ValidatorResults;
import me.myklebust.xpdoctor.validator.nodevalidator.BatchedQueryExecutor;

import com.enonic.xp.branch.Branches;
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

public class UniquePathValidatorExecutor
{
    private final NodeService nodeService;

    private final RepositoryService repositoryService;

    private final Set<NodePath> handledPaths = Sets.newHashSet();

    private final Logger LOG = LoggerFactory.getLogger( UniquePathValidatorExecutor.class );

    public UniquePathValidatorExecutor( final NodeService nodeService, final RepositoryService repositoryService )
    {
        this.nodeService = nodeService;
        this.repositoryService = repositoryService;
    }

    public ValidatorResults execute()
    {
        final BatchedQueryExecutor executor = BatchedQueryExecutor.create().
            batchSize( 5_000 ).
            nodeService( this.nodeService ).
            build();

        final ValidatorResults.Builder results = ValidatorResults.create();

        while ( executor.hasMore() )
        {
            results.add( checkNodes( executor.execute() ) );
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

        final NotUniquePathResult.Builder result = NotUniquePathResult.create( node.path() );

        final FindNodesByQueryResult queryResult = this.nodeService.findByQuery( NodeQuery.create().
            path( node.path() ).
            size( -1 ).
            build() );

        this.handledPaths.add( node.path() );

        if ( queryResult.getTotalHits() > 1 )
        {
            for ( final NodeId nodeId : queryResult.getNodeIds() )
            {
                addNotUniqueEntry( result, nodeId );
            }

            return result.build();
        }

        return null;
    }

    private void addNotUniqueEntry( final NotUniquePathResult.Builder result, final NodeId nodeId )
    {
        final Node foundNode = this.nodeService.getById( nodeId );

        final Repository repository = this.repositoryService.get( ContextAccessor.current().getRepositoryId() );

        final GetActiveNodeVersionsResult activeVersions = getBranches( nodeId, repository );

        result.add( NotUniqueEntry.create().
            nodeId( nodeId ).
            instant( foundNode.getTimestamp() ).
            branches( Branches.from( activeVersions.getNodeVersions().keySet() ) ).
            build() );
    }

    private GetActiveNodeVersionsResult getBranches( final NodeId nodeId, final Repository repository )
    {
        return this.nodeService.getActiveVersions( GetActiveNodeVersionsParams.create().
            nodeId( nodeId ).
            branches( repository.getBranches() ).
            build() );
    }


}
