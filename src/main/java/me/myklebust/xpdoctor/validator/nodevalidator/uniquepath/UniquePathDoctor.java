package me.myklebust.xpdoctor.validator.nodevalidator.uniquepath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepairResultImpl;
import me.myklebust.xpdoctor.validator.RepairStatus;

import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeName;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.node.RenameNodeParams;

public class UniquePathDoctor
{
    private final NodeService nodeService;

    private final Logger LOG = LoggerFactory.getLogger( UniquePathDoctor.class );

    private final static String PREFIX = "_com_enonic_app_xpdoctor";


    public UniquePathDoctor( final NodeService nodeService )
    {
        this.nodeService = nodeService;
    }

    public RepairResult repairNode( final NodeId nodeId, final boolean repairNow )
    {
        LOG.info( "Trying to repair node with non-unique path" );

        try
        {
            final Node nodeToBeRenamed = this.nodeService.getById( nodeId );

            final String newName = nodeToBeRenamed.name().toString() + PREFIX;
            this.nodeService.rename( RenameNodeParams.create().
                nodeId( nodeId ).
                nodeName( NodeName.from( newName ) ).
                build() );

            return RepairResultImpl.create().
                repairStatus( RepairStatus.REPAIRED ).
                message( String.format( "Node with id: %s renamed to %s", nodeId, newName ) ).
                build();
        }
        catch ( Exception e )
        {
            return RepairResultImpl.create().
                message( "Cannot repair node, exeption when trying to load" ).
                repairStatus( RepairStatus.FAILED ).
                build();
        }


    }

}

