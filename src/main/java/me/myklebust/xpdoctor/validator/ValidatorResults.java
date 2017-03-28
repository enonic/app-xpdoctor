package me.myklebust.xpdoctor.validator;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class ValidatorResults
{
    private final Multimap<ValidatorEntry, ValidationError> errors = HashMultimap.create();

    void add( final ValidatorEntry validatorEntry, final ValidationError error )
    {
        this.errors.put( validatorEntry, error );
    }


    public Multimap<ValidatorEntry, ValidationError> getErrors()
    {
        return errors;
    }

}
