package me.myklebust.xpdoctor.validator;

import java.util.List;

import com.enonic.xp.task.ProgressReporter;

public class AnalyzeParams
{
    private final ProgressReporter progressReporter;

    private final List<String> enabledValidators;

    private AnalyzeParams( final Builder builder )
    {
        progressReporter = builder.progressReporter;
        enabledValidators = builder.enabledValidators;
    }

    public static Builder create()
    {
        return new Builder();
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

        public AnalyzeParams build()
        {
            return new AnalyzeParams( this );
        }
    }
}
