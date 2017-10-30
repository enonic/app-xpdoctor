package me.myklebust.xpdoctor.validator;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.repository.RepositoryId;

public class RepairParams
{
    private NodeId nodeId;

    private String validatorName;

    private RepositoryId repoId;

    private Branch branch;

    public RepositoryId getRepoId()
    {
        return repoId;
    }

    public Branch getBranch()
    {
        return branch;
    }

    public void setRepoId( final String repoId )
    {
        this.repoId = RepositoryId.from( repoId );
    }

    public void setBranch( final String branch )
    {
        this.branch = Branch.from( branch );
    }

    public NodeId getNodeId()
    {
        return nodeId;
    }

    public void setNodeId( final String nodeId )
    {
        this.nodeId = NodeId.from( nodeId );
    }

    public String getValidatorName()
    {
        return validatorName;
    }

    public void setValidatorName( final String validatorName )
    {
        this.validatorName = validatorName;
    }
}
