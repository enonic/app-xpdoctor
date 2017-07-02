package me.myklebust.xpdoctor.validator;

import com.enonic.xp.branch.Branch;

public class BranchValidationResult
{

    private final Branch branch;

    private final ValidatorResults results;

    private BranchValidationResult( final Builder builder )
    {
        results = builder.results;
        branch = builder.branch;
    }

    public Branch getBranch()
    {
        return branch;
    }

    public ValidatorResults getResults()
    {
        return results;
    }

    public static Builder create( final Branch branch )
    {
        return new Builder( branch );
    }

    public static final class Builder
    {
        private ValidatorResults results;

        private final Branch branch;

        private Builder( final Branch branch )
        {
            this.branch = branch;
        }

        public Builder results( final ValidatorResults val )
        {
            results = val;
            return this;
        }

        public BranchValidationResult build()
        {
            return new BranchValidationResult( this );
        }
    }
}
