package me.myklebust.xpdoctor.validator;

import java.util.List;
import java.util.Map;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.branch.Branches;
import com.enonic.xp.repository.RepositoryId;
import com.enonic.xp.task.ProgressReporter;

public class AnalyzeParams
{
    private final ProgressReporter progressReporter;

    private final List<String> enabledValidators;

    private final Map<RepositoryId, Branches> repoBranches;

    private AnalyzeParams( final Builder builder )
    {
        progressReporter = builder.progressReporter;
        enabledValidators = builder.enabledValidators;
        repoBranches = builder.repoBranches;
    }

    public static Builder create()
    {
        return new Builder();
    }


    public Map<RepositoryId, Branches> getRepoBranches()
    {
        return repoBranches;
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

        private Map<RepositoryId, Branches> repoBranches;

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

        public Builder repoBranches( final Map<RepositoryId, Branches> val )
        {
            repoBranches = val;
            return this;
        }

        public AnalyzeParams build()
        {
            return new AnalyzeParams( this );
        }
    }
}
