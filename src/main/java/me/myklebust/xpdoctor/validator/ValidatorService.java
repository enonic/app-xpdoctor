package me.myklebust.xpdoctor.validator;

import java.util.List;

public interface ValidatorService
{
    RepoValidationResults analyze( AnalyzeParams params );

    List<Validator> getValidators();

    Validator getValidator( final String validatorName );
}
