package me.myklebust.xpdoctor.validator;

import java.util.List;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.repository.RepositoryId;
import com.enonic.xp.task.ProgressReporter;

public class AnalyzeParams
{
    private final ProgressReporter progressReporter;

    private final List<String> enabledValidators;

    private final Branch branch;

    private final RepositoryId repositoryId;


    private AnalyzeParams( final Builder builder )
    {
        progressReporter = builder.progressReporter;
        enabledValidators = builder.enabledValidators;
        branch = builder.branch;
        repositoryId = builder.repositoryId;
    }

    public static Builder create()
    {
        return new Builder();
    }


    public Branch getBranch()
    {
        return branch;
    }

    public RepositoryId getRepositoryId()
    {
        return repositoryId;
    }

    public ProgressReporter getProgressReporter()
    {
        return progressReporter;
    }

    public List<String> getEnabledValidators()
    {
        return enabledValidators;
    }

    public static final class Builder
    {
        private ProgressReporter progressReporter;

        private List<String> enabledValidators;

        private Branch branch;

        private RepositoryId repositoryId;

        private Builder()
        {
        }

        public Builder progressReporter( final ProgressReporter val )
        {
            progressReporter = val;
            return this;
        }

        public Builder enabledValidators( final List<String> val )
        {
            enabledValidators = val;
            return this;
        }

        public Builder branch( final Branch val )
        {
            branch = val;
            return this;
        }

        public Builder repositoryId( final RepositoryId val )
        {
            repositoryId = val;
            return this;
        }

        public AnalyzeParams build()
        {
            return new AnalyzeParams( this );
        }
    }
}
