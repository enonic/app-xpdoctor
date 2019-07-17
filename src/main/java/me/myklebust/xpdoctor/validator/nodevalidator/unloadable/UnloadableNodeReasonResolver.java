package me.myklebust.xpdoctor.validator.nodevalidator.unloadable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.myklebust.xpdoctor.storagespy.StorageSpyService;

import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.node.NodeId;

class UnloadableNodeReasonResolver
{

    private final StorageSpyService storageSpyService;

    private final static Logger LOG = LoggerFactory.getLogger( UnloadableNodeReasonResolver.class );

    public UnloadableNodeReasonResolver( final StorageSpyService storageSpyService )
    {
        this.storageSpyService = storageSpyService;
    }

    UnloadableReason resolve( final NodeId nodeId )
    {
        LOG.info( "Node with id " + nodeId + " loading failed, try to decide why....." );

        boolean inBranch =
            storageSpyService.existsInBranch( nodeId, ContextAccessor.current().getRepositoryId(), ContextAccessor.current().getBranch() );

        boolean inSearch =
            storageSpyService.existsInSearch( nodeId, ContextAccessor.current().getRepositoryId(), ContextAccessor.current().getBranch() );

        if ( !inBranch && !inSearch )
        {
            LOG.error( "Neither in branch or search, this should not appear at all" );
            return UnloadableReason.UNKNOWN;
        }

        if ( !inBranch )
        {
            return UnloadableReason.NOT_IN_STORAGE_BUT_IN_SEARCH;
        }

        // Check if blob is missing, for now just assume this
        return UnloadableReason.MISSING_BLOB;
    }

}
