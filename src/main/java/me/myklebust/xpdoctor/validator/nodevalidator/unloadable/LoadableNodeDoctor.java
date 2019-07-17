package me.myklebust.xpdoctor.validator.nodevalidator.unloadable;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteSource;

import me.myklebust.xpdoctor.storagespy.StorageSpyService;
import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepairResultImpl;
import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.nodevalidator.BatchedVersionExecutor;

import com.enonic.xp.blob.BlobKey;
import com.enonic.xp.blob.BlobRecord;
import com.enonic.xp.blob.BlobStore;
import com.enonic.xp.blob.Segment;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.node.NodeVersion;
import com.enonic.xp.node.NodeVersionMetadata;
import com.enonic.xp.node.NodeVersionsMetadata;

class LoadableNodeDoctor
{
    private final Logger LOG = LoggerFactory.getLogger( LoadableNodeDoctor.class );

    private final NodeService nodeService;

    private final BlobStore blobStore;

    private final StorageSpyService storageSpyService;

    LoadableNodeDoctor( final NodeService nodeService, final BlobStore blobStore, final StorageSpyService storageSpyService )
    {
        this.nodeService = nodeService;
        this.blobStore = blobStore;
        this.storageSpyService = storageSpyService;
    }

    RepairResult repairNode( final NodeId nodeId, final boolean repairNow, final UnloadableReason reason )
    {
        LOG.info( "Trying to repaid un-loadable node with id [" + nodeId + "]" );

        switch ( reason )
        {
            case MISSING_BLOB:
                return repairMissingBlob( nodeId, repairNow );
            case NOT_IN_STORAGE_BUT_IN_SEARCH:
                return repairMissingStorageButInSearch( nodeId, repairNow );
        }

        return RepairResultImpl.create().
            repairStatus( RepairStatus.UNKNOW ).
            message( "Not able to repair, unknown reason for node to be unloadable, check log" ).
            build();
    }

    private RepairResult repairMissingStorageButInSearch( final NodeId nodeId, final boolean repairNow )
    {
        if ( repairNow )
        {
            try
            {
                LOG.info( "Deleting node from search-index..." );

                final boolean deleted = this.storageSpyService.deleteInSearch( nodeId, ContextAccessor.current().getRepositoryId(),
                                                                               ContextAccessor.current().getBranch() );

                return RepairResultImpl.create().
                    repairStatus( RepairStatus.REPAIRED ).
                    message( "Deleted entry with id [" + nodeId + "] sin search-index: [" + deleted + "]").
                    build();
            }
            catch ( Exception e )
            {
                LOG.error( "Not able to delete entry from search-index", e );
                return RepairResultImpl.create().
                    repairStatus( RepairStatus.FAILED ).
                    message( "Failed to delete entry with id [ " + nodeId + " ] in search-index" ).
                    build();

            }

        }

        return RepairResultImpl.create().
            repairStatus( RepairStatus.IS_REPAIRABLE ).
            message( "Delete entry in search-index" ).
            build();
    }

    private RepairResult repairMissingBlob( final NodeId nodeId, final boolean repairNow )
    {
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

            String message = createMessage( workingVersion );

            if ( !repairNow )
            {
                return RepairResultImpl.create().
                    repairStatus( RepairStatus.IS_REPAIRABLE ).
                    message( message ).
                    build();
            }

            if ( workingVersion != null )
            {
                return doRollbackToVersion( nodeId, workingVersion );
            }
            else
            {
                return createMinimalNode( result );
            }
        }

        return RepairResultImpl.create().
            repairStatus( RepairStatus.NOT_REPAIRABLE ).
            message( "No working version found, checked " + executor.getTotalHits() + " versions" ).
            build();
    }

    private String createMessage( final NodeVersion workingVersion )
    {
        String message;

        if ( workingVersion != null )
        {
            message = String.format( "Working version found: id:[%s], timestamp:[%s]", workingVersion.getVersionId(),
                                     workingVersion.getTimestamp() );
            LOG.info( message );
        }
        else
        {
            message = "No working version found, repair will create a minimal node";
            LOG.info( message );
        }
        return message;
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

    private RepairResultImpl createMinimalNode( final NodeVersionsMetadata metadata )
    {
        final NodeVersionMetadata nodeVersionMetadata = metadata.iterator().next();

        final ByteSource byteSource = MinimalNodeFactory.create( "minimal_content.json", nodeVersionMetadata );

        try
        {
            final BlobRecord record = createBlobRecord( nodeVersionMetadata, byteSource );

            this.blobStore.addRecord( Segment.from( "node" ), record );

            LOG.info( "Blob record created for versionId: " + nodeVersionMetadata.getNodeVersionId() );
        }
        catch ( Exception e )
        {
            return RepairResultImpl.create().
                message( "Failed to created minimal blob: " + e.getMessage() ).
                repairStatus( RepairStatus.FAILED ).
                build();
        }

        return RepairResultImpl.create().
            message( "Created minimal node with " + nodeVersionMetadata.getNodeVersionId() ).
            repairStatus( RepairStatus.REPAIRED ).
            build();
    }

    private BlobRecord createBlobRecord( final NodeVersionMetadata nodeVersionMetadata, final ByteSource byteSource )
        throws IOException
    {
        LOG.info( "Creating blob-record for version " + nodeVersionMetadata.getNodeVersionId() );

        final long size = byteSource.size();

        return new BlobRecord()
        {
            @Override
            public BlobKey getKey()
            {
                return BlobKey.from( nodeVersionMetadata.getNodeVersionId().toString() );
            }

            @Override
            public long getLength()
            {
                return size;
            }

            @Override
            public ByteSource getBytes()
            {
                return byteSource;
            }

            @Override
            public long lastModified()
            {
                return nodeVersionMetadata.getTimestamp().toEpochMilli();
            }
        };
    }
}
