package me.myklebust.xpdoctor.validator.nodevalidator;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.repository.RepositoryId;

public class ProgressDescription
{
    private final String repositoryId;

    private final String branch;

    private final String validator;

    private ProgressDescription( final Builder builder )
    {
        repositoryId = builder.repositoryId.toString();
        branch = builder.branch.toString();
        validator = builder.validator;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public String getBranch()
    {
        return branch;
    }

    public String getValidator()
    {
        return validator;
    }

    public static Builder create()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private RepositoryId repositoryId;

        private Branch branch;

        private String validator;

        private Builder()
        {
        }

        public Builder repositoryId( final RepositoryId val )
        {
            repositoryId = val;
            return this;
        }

        public Builder branch( final Branch val )
        {
            branch = val;
            return this;
        }

        public Builder validator( final String validator )
        {
            this.validator = validator;
            return this;
        }

        public ProgressDescription build()
        {
            return new ProgressDescription( this );
        }
    }
}
