package me.myklebust.xpdoctor.validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class ValidatorResults
{
    private List<ValidatorResult> validatorResultsList;

    private ValidatorResults( final Builder builder )
    {
        validatorResultsList = builder.validatorResultsList;
    }

    public static Builder create()
    {
        return new Builder();
    }


    public List<ValidatorResult> getResults()
    {
        return validatorResultsList;
    }

    public static final class Builder
    {
        private List<ValidatorResult> validatorResultsList = new ArrayList<>();

        private Builder()
        {
        }

        public int size()
        {
            return validatorResultsList.size();
        }

        public Builder add( final ValidatorResult val )
        {
            validatorResultsList.add( val );
            return this;
        }

        public Builder add( final Collection<ValidatorResult> val )
        {
            validatorResultsList.addAll( val );
            return this;
        }

        public Builder add( final ValidatorResults val )
        {
            validatorResultsList.addAll( val.getResults() );
            return this;
        }

        public ValidatorResults build()
        {
            return new ValidatorResults( this );
        }
    }
}
