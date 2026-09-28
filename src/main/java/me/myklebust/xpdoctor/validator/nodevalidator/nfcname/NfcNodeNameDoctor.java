package me.myklebust.xpdoctor.validator.nodevalidator.nfcname;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.nodevalidator.NodeDoctor;

import com.enonic.xp.content.ContentConstants;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentName;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.content.RenameContentParams;
import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeName;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.node.RenameNodeParams;

public class NfcNodeNameDoctor
    implements NodeDoctor
{
    private static final Logger LOG = LoggerFactory.getLogger( NfcNodeNameDoctor.class );

    private final NodeService nodeService;

    private final ContentService contentService;

    public NfcNodeNameDoctor( final NodeService nodeService, final ContentService contentService )
    {
        this.nodeService = nodeService;
        this.contentService = contentService;
    }

    @Override
    public RepairResult repairNode( final NodeId nodeId, final boolean dryRun )
    {
        try
        {
            final Node node = this.nodeService.getById( nodeId );

            final String name = node.name().toString();
            final String normalized = Xp8NodeNames.normalize( name );

            if ( !Xp8NodeNames.isValid( normalized ) )
            {
                return result( RepairStatus.MANUAL, "Name contains characters that are not allowed in XP 8, rename the node manually" );
            }

            if ( normalized.equals( name ) )
            {
                return result( RepairStatus.NOT_NEEDED, "Node name is already valid" );
            }

            if ( isContent( node ) )
            {
                return renameContent( nodeId, normalized, dryRun );
            }

            if ( dryRun )
            {
                return result( RepairStatus.IS_REPAIRABLE, "Rename node to [" + normalized + "]" );
            }

            this.nodeService.rename( RenameNodeParams.create().nodeId( nodeId ).nodeName( NodeName.from( normalized ) ).build() );

            final String msg = String.format( "Node with id: %s renamed to %s in branch %s", nodeId, normalized,
                                              ContextAccessor.current().getBranch() );
            LOG.info( msg );
            return result( RepairStatus.REPAIRED, msg );
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to repair node", e );
            return result( RepairStatus.FAILED, "Cannot rename node: " + e.getMessage() );
        }
    }

    // Content is renamed through the content API, so layers stop inheriting the name and validation runs.
    // Only draft is renamed: master gets the new name when the content is published.
    private RepairResult renameContent( final NodeId nodeId, final String normalized, final boolean dryRun )
    {
        if ( !ContentConstants.BRANCH_DRAFT.equals( ContextAccessor.current().getBranch() ) )
        {
            return result( RepairStatus.MANUAL, "Rename content to [" + normalized + "] in draft and publish it" );
        }

        if ( dryRun )
        {
            return result( RepairStatus.IS_REPAIRABLE, "Rename content to [" + normalized + "], then publish it" );
        }

        this.contentService.rename(
            RenameContentParams.create().contentId( ContentId.from( nodeId.toString() ) ).newName( ContentName.from( normalized ) ).build() );

        final String msg = String.format( "Content with id: %s renamed to %s in draft, publish it to update master", nodeId, normalized );
        LOG.info( msg );
        return result( RepairStatus.REPAIRED, msg );
    }

    private static boolean isContent( final Node node )
    {
        final Context context = ContextAccessor.current();
        return context.getRepositoryId().toString().startsWith( ContentConstants.CONTENT_REPO_ID_PREFIX ) &&
            ContentConstants.CONTENT_NODE_COLLECTION.equals( node.getNodeType() );
    }

    private static RepairResult result( final RepairStatus status, final String message )
    {
        return RepairResult.create().repairStatus( status ).message( message ).build();
    }
}
