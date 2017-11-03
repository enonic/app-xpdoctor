package me.myklebust.xpdoctor.validator.nodevalidator.uniquepath;

import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.result.RepairResultImpl;
import me.myklebust.xpdoctor.validator.result.ValidatorResult;
import me.myklebust.xpdoctor.validator.result.ValidatorResultImpl;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.node.FindNodesByQueryResult;
import com.enonic.xp.node.GetActiveNodeVersionsParams;
import com.enonic.xp.node.GetActiveNodeVersionsResult;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeQuery;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.node.NodeVersionMetadata;
import com.enonic.xp.repository.Repository;
import com.enonic.xp.repository.RepositoryService;

import static me.myklebust.xpdoctor.validator.nodevalidator.uniquepath.UniquePathDoctor.PREFIX;

public class UniquePathEntryExecutor
{
    private final NodeService nodeService;

    private final RepositoryService repositoryService;

    protected final String validatorName;

    private UniquePathEntryExecutor( final Builder builder )
    {
        nodeService = builder.nodeService;
        repositoryService = builder.repositoryService;
        validatorName = builder.validatorName;
    }

    public ValidatorResult execute( final NodeId nodeId )
    {
        final Node node = this.nodeService.getById( nodeId );
        return execute( node );
    }

    public ValidatorResult execute( final Node node )
    {
        return doExecute( node );
    }

    private ValidatorResult doExecute( final Node node )
    {
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

        result.repairResult(
            RepairResultImpl.create().message( "rename to " + node.name() + PREFIX ).repairStatus( RepairStatus.IS_REPAIRABLE ).build() );

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

        return String.format( "id: [%s] in: [%s]", Joiner.on( "," ).join( ids ), Joiner.on( "," ).join( nodeVersions.keySet() ) );
    }

    private GetActiveNodeVersionsResult getBranches( final NodeId nodeId, final Repository repository )
    {
        return this.nodeService.getActiveVersions( GetActiveNodeVersionsParams.create().
            nodeId( nodeId ).
            branches( repository.getBranches() ).
            build() );
    }

    public static Builder create()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private NodeService nodeService;

        private RepositoryService repositoryService;

        private String validatorName;

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

        public Builder validatorName( final String val )
        {
            validatorName = val;
            return this;
        }

        public UniquePathEntryExecutor build()
        {
            return new UniquePathEntryExecutor( this );
        }
    }
}
