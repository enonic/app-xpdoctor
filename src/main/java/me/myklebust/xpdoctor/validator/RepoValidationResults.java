package me.myklebust.xpdoctor.validator;

import java.util.List;

import com.google.common.collect.Lists;

public class RepoValidationResults
{
    private final List<RepoValidationResult> repositories;

    private final Long timestamp;

    private RepoValidationResults( final Builder builder )
    {
        this.timestamp = System.currentTimeMillis();
        repositories = builder.repositories;
    }

    public static Builder create()
    {
        return new Builder();
    }

    public List<RepoValidationResult> getRepositories()
    {
        return repositories;
    }

    public Long getTimestamp()
    {
        return timestamp;
    }

    public static final class Builder
    {
        private List<RepoValidationResult> repositories = Lists.newArrayList();

        private Builder()
        {
        }

        public Builder add( final RepoValidationResult val )
        {
            this.repositories.add( val );
            return this;
        }

        public Builder repositories( final List<RepoValidationResult> val )
        {
            repositories = val;
            return this;
        }

        public RepoValidationResults build()
        {
            return new RepoValidationResults( this );
        }
    }
}
