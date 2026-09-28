package me.myklebust.xpdoctor.validator.nodevalidator.nfcname;

import java.text.Normalizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.nodevalidator.NodeDoctor;

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
            final String normalized = Normalizer.normalize( name, Normalizer.Form.NFC );

            if ( normalized.equals( name ) )
            {
                return RepairResult.create().
                    repairStatus( RepairStatus.NOT_NEEDED ).
                    message( "Node name is already NFC normalized" ).
                    build();
            }

            if ( !dryRun )
            {
                this.nodeService.rename( RenameNodeParams.create().
                    nodeId( nodeId ).
                    nodeName( NodeName.from( normalized ) ).
                    build() );
            }

            final String msg = String.format( "Node with id: %s renamed to %s", nodeId, normalized );
            LOG.info( msg );

            return RepairResult.create().
                repairStatus( RepairStatus.REPAIRED ).
                message( msg ).
                build();
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to repair node", e );

            return RepairResult.create().
                message( "Cannot rename node: " + e.getMessage() ).
                repairStatus( RepairStatus.FAILED ).
                build();
        }
    }
}
