package me.myklebust.xpdoctor.validator;

import java.util.Set;

import me.myklebust.xpdoctor.validator.model.IssueEntries;
import me.myklebust.xpdoctor.validator.result.RepoValidationResults;
import me.myklebust.xpdoctor.validator.result.ValidatorResults;

public interface ValidatorService
{
    RepoValidationResults analyze( AnalyzeParams params );

    ValidatorResults reAnalyze( final IssueEntries issues );

    Set<Validator> getValidators();

    Validator getValidator( final String validatorName );

}
