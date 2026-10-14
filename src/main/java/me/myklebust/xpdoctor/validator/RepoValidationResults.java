package me.myklebust.xpdoctor.validator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.repository.RepositoryId;

public class RepoValidationResults
{
    private final List<RepoValidationResult> repositories;

    private final Long timestamp;

    private final int totalIssues;

    private final Map<String, Issue> issues;

    private volatile long repairVersion = 0;

    private RepoValidationResults( final Builder builder )
    {
        this.timestamp = System.currentTimeMillis();
        repositories = builder.repositories;
        totalIssues = builder.totalIssues;
        issues = indexIssues( repositories );
    }

    private static Map<String, Issue> indexIssues( final List<RepoValidationResult> repositories )
    {
        final Map<String, Issue> issues = new LinkedHashMap<>();
        int issueNum = 0;
        for ( final RepoValidationResult repo : repositories )
        {
            for ( final BranchValidationResult branch : repo.getBranches() )
            {
                for ( final ValidatorResult result : branch.getResults().getResults() )
                {
                    final String issueId = "issue-" + ++issueNum;
                    issues.put( issueId, new Issue( issueId, repo.getRepositoryId(), branch.getBranch(), result ) );
                }
            }
        }
        return issues;
    }

    public Map<String, Issue> getIssues()
    {
        return issues;
    }

    public long getRepairVersion()
    {
        return repairVersion;
    }

    public void bumpRepairVersion()
    {
        this.repairVersion++;
    }

    public static final class Issue
    {
        private final String issueId;

        private final RepositoryId repositoryId;

        private final Branch branch;

        private final ValidatorResult result;

        private Issue( final String issueId, final RepositoryId repositoryId, final Branch branch, final ValidatorResult result )
        {
            this.issueId = issueId;
            this.repositoryId = repositoryId;
            this.branch = branch;
            this.result = result;
        }

        public String getIssueId()
        {
            return issueId;
        }

        public RepositoryId getRepositoryId()
        {
            return repositoryId;
        }

        public Branch getBranch()
        {
            return branch;
        }

        public ValidatorResult getResult()
        {
            return result;
        }
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
