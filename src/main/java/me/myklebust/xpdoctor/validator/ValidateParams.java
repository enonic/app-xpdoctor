package me.myklebust.xpdoctor.validator;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.repository.RepositoryId;

public class ValidateParams
{
    private List<String> enabledValidators;

    private RepositoryId repoId;

    private Branch branch;

    public void setRepoId( final String repoId )
    {
        this.repoId = RepositoryId.from( repoId );
    }

    public void setBranch( final String branch )
    {
        this.branch = Branch.from( branch );
    }

    public RepositoryId getRepoId()
    {
        return repoId;
    }

    public Branch getBranch()
    {
        return branch;
    }

    public void setEnabledValidators( final List<String> enabledValidators )
    {
        this.enabledValidators = enabledValidators;
    }

    public List<String> getEnabledValidators()
    {
        return enabledValidators;
    }

    public void setEnabledValidators( final String[] enabledValidators )
    {

        this.enabledValidators = Arrays.asList( enabledValidators );
    }

    public void setEnabledValidators( final String enabledValidator )
    {

        this.enabledValidators = Lists.newArrayList( enabledValidator );
    }

}
