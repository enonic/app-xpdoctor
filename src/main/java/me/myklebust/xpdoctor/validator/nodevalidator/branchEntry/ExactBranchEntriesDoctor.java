package me.myklebust.xpdoctor.validator.nodevalidator.branchEntry;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.nodevalidator.NodeDoctor;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.branch.Branches;
import com.enonic.xp.content.ContentConstants;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.node.GetActiveNodeVersionsParams;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeIds;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.node.NodeVersionMetadata;
import com.enonic.xp.node.PushNodesResult;

public class ExactBranchEntriesDoctor
    implements NodeDoctor
{
    private static final Logger LOG = LoggerFactory.getLogger( ExactBranchEntriesDoctor.class );

    private final NodeService nodeService;

    public ExactBranchEntriesDoctor( final NodeService nodeService )
    {
        this.nodeService = nodeService;
    }

    @Override
    public RepairResult repairNode( final NodeId nodeId, final boolean dryRun )
    {
        LOG.info( "Trying to repair node with equal branch entries, nodeId: {}", nodeId );

        if ( dryRun )
        {
            return result( RepairStatus.IS_REPAIRABLE, String.format( "Push node with id: %s from draft to master", nodeId ) );
        }

        try
        {
            // The issue is reported in both the draft and the master scan. Push always takes its source from the context
            // branch, so pushing in the master context would push master onto itself and change nothing.
            final PushNodesResult result = ContextBuilder.from( ContextAccessor.current() )
                .branch( ContentConstants.BRANCH_DRAFT )
                .build()
                .callWith( () -> this.nodeService.push( NodeIds.from( nodeId ), ContentConstants.BRANCH_MASTER ) );

            if ( !result.getSuccessful().isNotEmpty() )
            {
                return result( RepairStatus.FAILED, String.format( "Node with id: %s could not be pushed to master. %s", nodeId,
                                                                   result.getFailed()
                                                                       .stream()
                                                                       .findFirst()
                                                                       .map( f -> f.getReason().toString() )
                                                                       .orElse( "No details available" ) ) );
            }

            final Map<Branch, NodeVersionMetadata> versions = this.nodeService.getActiveVersions( GetActiveNodeVersionsParams.create()
                                                                                              .nodeId( nodeId )
                                                                                              .branches( Branches.from(
                                                                                                  ContentConstants.BRANCH_DRAFT,
                                                                                                  ContentConstants.BRANCH_MASTER ) )
                                                                                              .build() ).getNodeVersions();
            final NodeVersionMetadata draft = versions.get( ContentConstants.BRANCH_DRAFT );
            final NodeVersionMetadata master = versions.get( ContentConstants.BRANCH_MASTER );

            if ( draft == null || master == null || !draft.getNodeVersionId().equals( master.getNodeVersionId() ) )
            {
                return result( RepairStatus.FAILED,
                               String.format( "Node with id: %s was pushed, but master still has another version than draft", nodeId ) );
            }

            return result( RepairStatus.REPAIRED,
                           String.format( "Node with id: %s pushed to master, both branches have version %s", nodeId,
                                          master.getNodeVersionId() ) );
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to repair node", e );
            return result( RepairStatus.FAILED, "Cannot repair node, exception when trying to push: " + e.getMessage() );
        }
    }

    private static RepairResult result( final RepairStatus status, final String message )
    {
        return RepairResult.create().repairStatus( status ).message( message ).build();
    }
}
