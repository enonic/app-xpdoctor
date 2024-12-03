package me.myklebust.xpdoctor.storagespy;

import org.elasticsearch.action.get.GetResponse;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeVersionId;
import com.enonic.xp.repository.RepositoryId;

public interface StorageSpyService
{
    boolean existsInBranch( final NodeId nodeId, final RepositoryId repositoryId, final Branch branch );

    GetResponse getInBranch( final NodeId nodeId, final RepositoryId repositoryId, final Branch branch );

    GetResponse getVersion( final NodeId nodeId, final NodeVersionId nodeVersionId, final RepositoryId repositoryId );

    boolean existsInSearch( final NodeId nodeId, final RepositoryId repositoryId, final Branch branch );

    boolean deleteInSearch( final NodeId nodeId, final RepositoryId repositoryId, final Branch branch );
}

