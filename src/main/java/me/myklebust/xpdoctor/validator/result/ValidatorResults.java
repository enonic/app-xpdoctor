package me.myklebust.xpdoctor.validator.result;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;

public class ValidatorResults
    implements Iterable<ValidatorResult>
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


    @Override
    public Iterator<ValidatorResult> iterator()
    {
        return this.validatorResultsList.iterator();
    }

    public List<ValidatorResult> getResults()
    {
        return validatorResultsList;
    }

    public static final class Builder
    {
        private List<ValidatorResult> validatorResultsList = Lists.newArrayList();

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
