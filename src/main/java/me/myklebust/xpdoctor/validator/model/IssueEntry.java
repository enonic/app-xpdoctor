package me.myklebust.xpdoctor.validator.model;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.repository.RepositoryId;

public class IssueEntry
{
    private NodeId nodeId;

    private Branch branch;

    private RepositoryId repoId;

    public NodeId getNodeId()
    {
        return nodeId;
    }

    public void setNodeId( final String nodeId )
    {
        this.nodeId = NodeId.from( nodeId );
    }

    public Branch getBranch()
    {
        return branch;
    }

    public void setBranch( final String branch )
    {
        this.branch = Branch.from( branch );
    }

    public RepositoryId getRepoId()
    {
        return repoId;
    }

    public void setRepoId( final String repoId )
    {
        this.repoId = RepositoryId.from( repoId );
    }
}
