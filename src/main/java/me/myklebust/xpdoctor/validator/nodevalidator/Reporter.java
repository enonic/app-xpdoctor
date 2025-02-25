package me.myklebust.xpdoctor.validator.nodevalidator;

import java.io.UncheckedIOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import me.myklebust.xpdoctor.json.ObjectMapperHelper;
import me.myklebust.xpdoctor.validator.ValidatorResult;
import me.myklebust.xpdoctor.validator.ValidatorResults;

import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.task.ProgressReporter;

public class Reporter
{
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperHelper.create();

    final ProgressReporter progressReporter;

    public final String validatorName;

    final ValidatorResults.Builder results = ValidatorResults.create();

    public Reporter( final String validatorName, ProgressReporter progressReporter )
    {
        this.validatorName = validatorName;
        this.progressReporter = progressReporter;
    }

    public void addResult( final ValidatorResult.Builder result )
    {
        this.results.add( result.validatorName( this.validatorName ).build() );
    }

    public ValidatorResults buildResults()
    {
        return this.results.build();
    }

    public void reportStart()
    {
        final Context context = ContextAccessor.current();

        final ProgressDescription description = ProgressDescription.create().
            branch( context.getBranch() ).
            repositoryId( context.getRepositoryId() ).
            validator( validatorName ).
            build();

        try
        {
            final String value = OBJECT_MAPPER.writeValueAsString( description );
            progressReporter.info( value );
        }
        catch ( JsonProcessingException e )
        {
            throw new UncheckedIOException( e );
        }
    }

    public ProgressReporter getProgressReporter()
    {
        return progressReporter;
    }
}
