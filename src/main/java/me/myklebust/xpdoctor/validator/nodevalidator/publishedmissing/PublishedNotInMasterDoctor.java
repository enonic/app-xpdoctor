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
import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.node.GetActiveNodeVersionsParams;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeIds;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.node.NodeVersion;
import com.enonic.xp.node.PushNodeParams;
import com.enonic.xp.node.PushNodeResult;
import com.enonic.xp.node.PushNodesResult;

/**
 * Imported content lands in draft only, so master usually lacks its parents too. The node is pushed together with every
 * ancestor that is missing in master: push stores them sorted by path, so parents are in place before their children.
 */
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
        try
        {
            final Context draftContext = ContextBuilder.from( ContextAccessor.current() ).branch( ContentConstants.BRANCH_DRAFT ).build();

            final NodeIds missingAncestors = draftContext.callWith( () -> findAncestorsMissingInMaster( nodeId ) );

            if ( dryRun )
            {
                return result( RepairStatus.IS_REPAIRABLE, missingAncestors.isEmpty()
                    ? "Push the current draft version to master"
                    : String.format( "Push the current draft version to master, with %s parent(s) missing in master", missingAncestors.getSize() ) );
            }

            // Plain push, without the publish processor: master gets the exact draft versions, no new versions are created
            final PushNodesResult result = draftContext.callWith( () -> this.nodeService.push( PushNodeParams.create()
                                                                                                    .ids( NodeIds.create()
                                                                                                              .addAll( missingAncestors )
                                                                                                              .add( nodeId )
                                                                                                              .build() )
                                                                                                    .target( ContentConstants.BRANCH_MASTER )
                                                                                                    .build() ) );

            final PushNodeResult failed =
                result.getFailed().stream().filter( f -> nodeId.equals( f.getNodeId() ) ).findFirst().orElse( null );
            if ( failed != null )
            {
                return result( RepairStatus.FAILED,
                               String.format( "Node with id: %s could not be pushed to master: %s", nodeId, failed.getFailureReason() ) );
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

            final String msg =
                String.format( "Node with id: %s pushed to master with version %s, together with %s missing parent(s)", nodeId,
                               master.getNodeVersionId(), missingAncestors.getSize() );
            LOG.info( msg );
            return result( RepairStatus.REPAIRED, msg );
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to repair node", e );
            return result( RepairStatus.FAILED, "Cannot repair node, exception when trying to push: " + e.getMessage() );
        }
    }

    private NodeIds findAncestorsMissingInMaster( final NodeId nodeId )
    {
        final Context masterContext = ContextBuilder.from( ContextAccessor.current() ).branch( ContentConstants.BRANCH_MASTER ).build();

        final NodeIds.Builder missing = NodeIds.create();
        NodePath parentPath = this.nodeService.getById( nodeId ).path().getParentPath();
        while ( !parentPath.isRoot() )
        {
            final NodePath path = parentPath;
            if ( masterContext.callWith( () -> this.nodeService.nodeExists( path ) ) )
            {
                break;
            }
            final Node parent = this.nodeService.getByPath( path );
            if ( parent == null )
            {
                // Missing in draft as well: push will fail with PARENT_NOT_FOUND and report it
                break;
            }
            missing.add( parent.id() );
            parentPath = path.getParentPath();
        }
        return missing.build();
    }

    private static RepairResult result( final RepairStatus status, final String message )
    {
        return RepairResult.create().repairStatus( status ).message( message ).build();
    }
}
