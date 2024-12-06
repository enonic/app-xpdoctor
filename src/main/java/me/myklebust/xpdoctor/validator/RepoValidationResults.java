package me.myklebust.xpdoctor.validator;

import java.util.ArrayList;
import java.util.List;

public class RepoValidationResults
{
    private final List<RepoValidationResult> repositories;

    private final Long timestamp;

    private final int totalIssues;

    private RepoValidationResults( final Builder builder )
    {
        this.timestamp = System.currentTimeMillis();
        repositories = builder.repositories;
        totalIssues = builder.totalIssues;
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

    public int getTotalIssues()
    {
        return totalIssues;
    }

    public static final class Builder
    {
        private List<RepoValidationResult> repositories = new ArrayList<>();

        private int totalIssues = 0;

        private Builder()
        {
        }

        public Builder add( final RepoValidationResult val )
        {
            this.repositories.add( val );
            totalIssues += val.getTotalIssues();
            return this;
        }

        public RepoValidationResults build()
        {
            return new RepoValidationResults( this );
        }
    }
}
