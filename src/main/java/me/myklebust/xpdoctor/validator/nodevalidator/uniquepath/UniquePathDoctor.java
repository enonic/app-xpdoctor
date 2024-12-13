package me.myklebust.xpdoctor.validator.nodevalidator.uniquepath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.nodevalidator.NodeDoctor;

import com.enonic.xp.node.FindNodesByQueryResult;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeName;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.node.NodeQuery;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.node.RefreshMode;
import com.enonic.xp.node.RenameNodeParams;

public class UniquePathDoctor
    implements NodeDoctor
{
    private static final Logger LOG = LoggerFactory.getLogger( UniquePathDoctor.class );

    private final NodeService nodeService;

    public final static String PREFIX = "_com_enonic_app_xpdoctor";

    public UniquePathDoctor( final NodeService nodeService )
    {
        this.nodeService = nodeService;
    }

    public RepairResult repairNode( final NodeId nodeId, final boolean dryRun )
    {
        LOG.info( "Trying to repair node with non-unique path" );
        this.nodeService.refresh( RefreshMode.ALL );

        try
        {
            final Node nodeToBeRenamed = this.nodeService.getById( nodeId );

            final NodePath nonUniquePath = nodeToBeRenamed.path();

            final FindNodesByQueryResult nodes = nodeService.findByQuery( NodeQuery.create().
                path( nonUniquePath ).
                build() );

            if ( nodes.getHits() <= 1 )
            {
                return RepairResult.create().
                    repairStatus( RepairStatus.NOT_NEEDED ).
                    message( "Path no longer non-uniquer" ).
                    build();
            }

            final String newName = doRename( nodeToBeRenamed );

            return RepairResult.create().
                repairStatus( RepairStatus.REPAIRED ).
                message( String.format( "Node with id: %s renamed to %s", nodeId, newName ) ).
                build();
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to repair node", e );

            return RepairResult.create().
                message( "Cannot repair node, exeption when trying to load: " + e.getMessage() ).
                repairStatus( RepairStatus.FAILED ).
                build();
        }


    }

    private String doRename( final Node nodeToBeRenamed )
    {
        final String newName = nodeToBeRenamed.name().toString() + PREFIX;
        this.nodeService.rename( RenameNodeParams.create().
            nodeId( nodeToBeRenamed.id() ).
            nodeName( NodeName.from( newName ) ).
            build() );
        return newName;
    }

}

