package me.myklebust.xpdoctor.validator;

import java.util.Set;

public interface ValidatorService
{
    RepoValidationResults execute( ValidatorParams params );

    Set<Validator> getValidators();

}
