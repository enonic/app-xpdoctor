package me.myklebust.xpdoctor.validator.nodevalidator.parentexists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.nodevalidator.NodeDoctor;

import com.enonic.xp.node.CreateNodeParams;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.node.NodeService;

public class NoParentDoctor implements NodeDoctor
{
    private static final Logger LOG = LoggerFactory.getLogger( NoParentDoctor.class );

    private final NodeService nodeService;

    public final static String LOST_FOLDER = "_com_enonic_app_xpdoctor";

    public NoParentDoctor( final NodeService nodeService )
    {
        this.nodeService = nodeService;
    }

    @Override
    public RepairResult repairNode( final NodeId nodeId, final boolean dryRun )
    {
        LOG.info( "Moving node to default lost-folder" );

        try
        {
            final Node nodeToBeMoved = this.nodeService.getById( nodeId );

            final NodePath parentPath = nodeToBeMoved.parentPath();

            final Node parent = this.nodeService.getByPath( parentPath );

            if ( parent == null )
            {
                final Node newParentNode = this.nodeService.create( CreateNodeParams.create().
                    name( parentPath.getName() ).
                    parent( parentPath.getParentPath() ).
                    build() );

                final String msg = "Created new parent with id: " + newParentNode.id() + " and path [" + newParentNode.path() + "]";
                LOG.info( msg );

                return RepairResult.create().
                    message( msg ).
                    repairStatus( RepairStatus.REPAIRED ).
                    build();
            }
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to repair node", e );

            return RepairResult.create().
                message( "Cannot repair node, exception when trying to load: " + e.getMessage() ).
                repairStatus( RepairStatus.FAILED ).
                build();
        }

        return RepairResult.create().
            message( "No need to repair, parent already exists?" ).
            repairStatus( RepairStatus.UNKNOW ).
            build();
    }

}
