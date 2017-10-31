package me.myklebust.xpdoctor.validator.nodevalidator.unloadable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepairResultImpl;
import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.nodevalidator.BatchedVersionExecutor;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.node.NodeVersion;
import com.enonic.xp.node.NodeVersionMetadata;
import com.enonic.xp.node.NodeVersionsMetadata;

class LoadableNodeDoctor
{
    private final Logger LOG = LoggerFactory.getLogger( LoadableNodeDoctor.class );

    private final NodeService nodeService;

    LoadableNodeDoctor( final NodeService nodeService )
    {
        this.nodeService = nodeService;
    }

    RepairResult repaidNode( final NodeId nodeId, final boolean repairNow )
    {
        LOG.info( "Trying to repaid un-loadable node with id [" + nodeId + "]" );
        LOG.info( "Checking for older versions of node with id: [" + nodeId + "]......" );

        final BatchedVersionExecutor executor = BatchedVersionExecutor.create( this.nodeService ).
            nodeId( nodeId ).
            batchSize( 10 ).
            build();

        LOG.info( "Found: " + executor.getTotalHits() + " versions of node" );

        while ( executor.hasMore() )
        {
            final NodeVersionsMetadata result = executor.execute();

            final NodeVersion workingVersion = findNewestWorkingVersion( result );

            if ( workingVersion != null )
            {
                final String message = String.format( "Working version found: id:[%s], timestamp:[%s]", workingVersion.getVersionId(),
                                                      workingVersion.getTimestamp() );
                LOG.info( message );

                if ( repairNow )
                {
                    return doRollbackToVersion( nodeId, workingVersion );
                }
                else
                {
                    return RepairResultImpl.create().
                        repairStatus( RepairStatus.IS_REPAIRABLE ).
                        message( message ).
                        build();
                }
            }
        }

        return RepairResultImpl.create().
            repairStatus( RepairStatus.NOT_REPAIRABLE ).
            message( "No working version found, checked " + executor.getTotalHits() + " versions" ).
            build();
    }

    private NodeVersion findNewestWorkingVersion( final NodeVersionsMetadata result )
    {
        for ( final NodeVersionMetadata version : result )
        {
            try
            {
                final NodeVersion byNodeVersion = this.nodeService.getByNodeVersion( version.getNodeVersionId() );
                if ( byNodeVersion != null )
                {
                    return byNodeVersion;
                }
            }
            catch ( Exception e )
            {
                final String message =
                    String.format( "Trying version with id: [%s], path: [%s], timestamp: [%s] - Not found", version.getNodeVersionId(),
                                   version.getNodePath(), version.getTimestamp() );
                LOG.info( message );
            }
        }

        return null;
    }

    private RepairResultImpl doRollbackToVersion( final NodeId nodeId, final NodeVersion byNodeVersion )
    {
        try
        {
            this.nodeService.setActiveVersion( nodeId, byNodeVersion.getVersionId() );
            final String message = "Successfully restored version from [" + byNodeVersion.getTimestamp() + "]";
            LOG.info( message );
            return RepairResultImpl.create().
                repairStatus( RepairStatus.REPAIRED ).
                message( message ).
                build();
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to roll-back version", e );
            return RepairResultImpl.create().
                repairStatus( RepairStatus.FAILED ).
                message( "Failed to roll-back version: " + e.toString() ).
                build();
        }
    }
}
