package me.myklebust.xpdoctor.validator.nodevalidator.publishedmissing;

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
import com.enonic.xp.node.NodeVersion;
import com.enonic.xp.node.PushNodeParams;
import com.enonic.xp.node.PushNodesResult;

public class PublishedNotInMasterDoctor
    implements NodeDoctor
{
    private static final Logger LOG = LoggerFactory.getLogger( PublishedNotInMasterDoctor.class );

    private final NodeService nodeService;

    public PublishedNotInMasterDoctor( final NodeService nodeService )
    {
        this.nodeService = nodeService;
    }

    @Override
    public RepairResult repairNode( final NodeId nodeId, final boolean dryRun )
    {
        if ( dryRun )
        {
            return result( RepairStatus.IS_REPAIRABLE, String.format( "Push the current draft version of node %s to master", nodeId ) );
        }

        try
        {
            // Plain push, without the publish processor: master gets the exact draft version, no new version is created
            final PushNodesResult result = ContextBuilder.from( ContextAccessor.current() )
                .branch( ContentConstants.BRANCH_DRAFT )
                .build()
                .callWith( () -> this.nodeService.push(
                    PushNodeParams.create().ids( NodeIds.from( nodeId ) ).target( ContentConstants.BRANCH_MASTER ).build() ) );

            if ( result.getSuccessful().isEmpty() )
            {
                return result( RepairStatus.FAILED, String.format( "Node with id: %s could not be pushed to master. %s", nodeId,
                                                                   result.getFailed()
                                                                       .stream()
                                                                       .findFirst()
                                                                       .map( f -> f.getFailureReason().toString() )
                                                                       .orElse( "No details available" ) ) );
            }

            final Map<Branch, NodeVersion> versions = this.nodeService.getActiveVersions( GetActiveNodeVersionsParams.create()
                                                                                              .nodeId( nodeId )
                                                                                              .branches( Branches.from(
                                                                                                  ContentConstants.BRANCH_DRAFT,
                                                                                                  ContentConstants.BRANCH_MASTER ) )
                                                                                              .build() ).getNodeVersions();
            final NodeVersion draft = versions.get( ContentConstants.BRANCH_DRAFT );
            final NodeVersion master = versions.get( ContentConstants.BRANCH_MASTER );

            if ( draft == null || master == null || !draft.getNodeVersionId().equals( master.getNodeVersionId() ) )
            {
                return result( RepairStatus.FAILED,
                               String.format( "Node with id: %s was pushed, but master does not have the draft version", nodeId ) );
            }

            final String msg = String.format( "Node with id: %s pushed to master with version %s", nodeId, master.getNodeVersionId() );
            LOG.info( msg );
            return result( RepairStatus.REPAIRED, msg );
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
