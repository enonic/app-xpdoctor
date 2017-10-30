package me.myklebust.xpdoctor.validator;

import java.util.Set;

public interface ValidatorService
{
    RepoValidationResults analyze( AnalyzeParams params );

    Set<Validator> getValidators();

    Validator getValidator( final String validatorName );

}
