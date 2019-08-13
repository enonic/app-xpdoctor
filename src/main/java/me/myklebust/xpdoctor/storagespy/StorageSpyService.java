package me.myklebust.xpdoctor.storagespy;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.repository.RepositoryId;

public interface StorageSpyService
{
    boolean existsInBranch( final NodeId nodeId, final RepositoryId repositoryId, final Branch branch );

    boolean existsInSearch( final NodeId nodeId, final RepositoryId repositoryId, final Branch branch );

    boolean deleteInSearch( final NodeId nodeId, final RepositoryId repositoryId, final Branch branch );
}

