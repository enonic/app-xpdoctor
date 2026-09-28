package me.myklebust.xpdoctor.validator.nodevalidator.nfcname;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.nodevalidator.NodeDoctor;

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

    public NfcNodeNameDoctor( final NodeService nodeService )
    {
        this.nodeService = nodeService;
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

            if ( dryRun )
            {
                return result( RepairStatus.IS_REPAIRABLE, "Rename node to [" + normalized + "]" );
            }

            // Renames the node, also in content repositories, and in the current branch only: this is exactly what
            // the XP 8 dump upgrade does to the name. The content API would stop name inheritance in layers.
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

    private static RepairResult result( final RepairStatus status, final String message )
    {
        return RepairResult.create().repairStatus( status ).message( message ).build();
    }
}
