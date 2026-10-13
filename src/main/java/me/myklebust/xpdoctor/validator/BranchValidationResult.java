package me.myklebust.xpdoctor.validator;

import com.enonic.xp.branch.Branch;

public class BranchValidationResult
{

    private final Branch branch;

    private final ValidatorResults results;

    private final int nodeCount;

    private BranchValidationResult( final Builder builder )
    {
        results = builder.results;
        branch = builder.branch;
        nodeCount = builder.nodeCount;
    }

    public Branch getBranch()
    {
        return branch;
    }

    public ValidatorResults getResults()
    {
        return results;
    }

    public int getNodeCount()
    {
        return nodeCount;
    }

    public static Builder create( final Branch branch )
    {
        return new Builder( branch );
    }

    public static final class Builder
    {
        private ValidatorResults results;

        private final Branch branch;

        private int nodeCount;

        private Builder( final Branch branch )
        {
            this.branch = branch;
        }

        public Builder results( final ValidatorResults val )
        {
            results = val;
            return this;
        }

        public Builder nodeCount( final int val )
        {
            nodeCount = val;
            return this;
        }

        public BranchValidationResult build()
        {
            return new BranchValidationResult( this );
        }
    }
}
