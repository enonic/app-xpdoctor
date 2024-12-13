package me.myklebust.xpdoctor.validator.nodevalidator.unloadable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.myklebust.xpdoctor.validator.StorageSpyService;
import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.nodevalidator.BatchedVersionExecutor;
import me.myklebust.xpdoctor.validator.nodevalidator.NodeDoctor;

import com.enonic.xp.blob.BlobStore;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.node.NodeVersion;
import com.enonic.xp.node.NodeVersionMetadata;
import com.enonic.xp.node.NodeVersionsMetadata;

class LoadableNodeDoctor
implements NodeDoctor
{
    private static final Logger LOG = LoggerFactory.getLogger( LoadableNodeDoctor.class );

    private final NodeService nodeService;

    private final BlobStore blobStore;

    private final StorageSpyService storageSpyService;

    private final UnloadableNodeReasonResolver reasonResolver;

    LoadableNodeDoctor( final NodeService nodeService, final BlobStore blobStore, final StorageSpyService storageSpyService )
    {
        this.nodeService = nodeService;
        this.blobStore = blobStore;
        this.storageSpyService = storageSpyService;
        this.reasonResolver = new UnloadableNodeReasonResolver( storageSpyService );
    }

    @Override
    public RepairResult repairNode( final NodeId nodeId, final boolean dryRun )
    {
        LOG.info( "Trying to repair un-loadable node with id [{}]", nodeId );
        try
        {
            UnloadableReason reason = reasonResolver.resolve( nodeId );
            switch ( reason )
            {
                case MISSING_BLOB:
                    return repairMissingBlob( nodeId, dryRun );
                case NOT_IN_STORAGE_BUT_IN_SEARCH:
                    return repairMissingStorageButInSearch( nodeId, dryRun );
            }
        }
        catch ( Exception e )
        {
            LOG.error( "Not able to resolve reason for unloadable node", e );
        }

        return RepairResult.create().
            repairStatus( RepairStatus.UNKNOW ).
            message( "Not able to repair, unknown reason for node to be unloadable, check log" ).
            build();
    }

    private RepairResult repairMissingStorageButInSearch( final NodeId nodeId, final boolean dryRun )
    {
        if ( !dryRun )
        {
            try
            {
                LOG.info( "Deleting node from search-index..." );

                final boolean deleted = this.storageSpyService.deleteInSearch( nodeId, ContextAccessor.current().getRepositoryId(),
                                                                               ContextAccessor.current().getBranch() );

                return RepairResult.create().
                    repairStatus( RepairStatus.REPAIRED ).
                    message( "Deleted entry with id [" + nodeId + "] in search-index: [" + deleted + "]" ).
                    build();
            }
            catch ( Exception e )
            {
                LOG.error( "Not able to delete entry from search-index", e );
                return RepairResult.create().
                    repairStatus( RepairStatus.FAILED ).
                    message( "Failed to delete entry with id [ " + nodeId + " ] in search-index" ).
                    build();
            }
        }

        return RepairResult.create().
            repairStatus( RepairStatus.IS_REPAIRABLE ).
            message( "Delete entry in search-index" ).
            build();
    }

    private RepairResult repairMissingBlob( final NodeId nodeId, final boolean dryRun )
    {
        LOG.info( "Checking for older versions of node with id: [{}]......", nodeId );

        final BatchedVersionExecutor executor = BatchedVersionExecutor.create( this.nodeService ).
            nodeId( nodeId ).
            batchSize( 10 ).
            build();

        LOG.info( "Found: {} versions of node", executor.getTotalHits() );

        while ( executor.hasMore() )
        {
            final NodeVersionsMetadata result = executor.execute();

            final NodeVersionMetadata workingVersionMetadata = findNewestWorkingVersion( result );

            String message = createMessage( workingVersionMetadata );

            if ( dryRun )
            {
                return RepairResult.create().
                    repairStatus( RepairStatus.IS_REPAIRABLE ).
                    message( message ).
                    build();
            }

            if ( workingVersionMetadata != null )
            {
                return doRollbackToVersion( nodeId, workingVersionMetadata );
            }
        }

        return RepairResult.create().
            repairStatus( RepairStatus.NOT_REPAIRABLE ).
            message( "No working version found, checked " + executor.getTotalHits() + " versions" ).
            build();
    }

    private String createMessage( final NodeVersionMetadata workingVersionMetadata )
    {
        String message;

        if ( workingVersionMetadata != null )
        {
            message = String.format( "Working version found: id:[%s], timestamp:[%s]",
                                     workingVersionMetadata.getNodeVersionKey().getNodeBlobKey(), workingVersionMetadata.getTimestamp() );
            LOG.info( message );
        }
        else
        {
            message = "No working version found, repair will create a minimal node";
            LOG.info( message );
        }
        return message;
    }

    private NodeVersionMetadata findNewestWorkingVersion( final NodeVersionsMetadata result )
    {
        for ( final NodeVersionMetadata version : result )
        {
            try
            {
                final NodeVersion byNodeVersion = this.nodeService.getByNodeVersionKey( version.getNodeVersionKey() );
                if ( byNodeVersion != null )
                {
                    return version;
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

    private RepairResult doRollbackToVersion( final NodeId nodeId, final NodeVersionMetadata nodeVersionMetadata )
    {
        try
        {
            this.nodeService.setActiveVersion( nodeId, nodeVersionMetadata.getNodeVersionId() );
            final String message = "Successfully restored version from [" + nodeVersionMetadata.getTimestamp() + "]";
            LOG.info( message );
            return RepairResult.create().
                repairStatus( RepairStatus.REPAIRED ).
                message( message ).
                build();
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to roll-back version", e );
            return RepairResult.create().
                repairStatus( RepairStatus.FAILED ).
                message( "Failed to roll-back version: " + e.getMessage() ).
                build();
        }
    }
}
