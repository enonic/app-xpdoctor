package me.myklebust.xpdoctor.validator;

import java.util.List;

import com.google.common.collect.Lists;

import com.enonic.xp.repository.RepositoryId;

public class RepoValidationResult
{
    private final RepositoryId repositoryId;

    private final List<BranchValidationResult> branches;

    private final int totalIssues;

    private RepoValidationResult( final Builder builder )
    {
        repositoryId = builder.repositoryId;
        branches = builder.branches;
        totalIssues = builder.totalIssues;
    }

    public RepositoryId getRepositoryId()
    {
        return repositoryId;
    }

    public List<BranchValidationResult> getBranches()
    {
        return branches;
    }

    public int getTotalIssues()
    {
        return totalIssues;
    }

    public static Builder create( final RepositoryId repoId )
    {
        return new Builder( repoId );
    }

    public static final class Builder
    {
        private final RepositoryId repositoryId;

        private List<BranchValidationResult> branches = Lists.newArrayList();

        private int totalIssues = 0;

        private Builder( final RepositoryId repoId )
        {
            this.repositoryId = repoId;
        }

        public Builder add( final BranchValidationResult val )
        {
            branches.add( val );
            totalIssues += val.getResults().getResults().size();
            return this;
        }

        public RepoValidationResult build()
        {
            return new RepoValidationResult( this );
        }
    }
}
