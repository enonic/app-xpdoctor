package me.myklebust.xpdoctor.validator.nodevalidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import me.myklebust.xpdoctor.validator.ValidatorResult;
import me.myklebust.xpdoctor.validator.ValidatorResults;

import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.task.ProgressReporter;

public class Reporter
{
    private static final Logger LOG = LoggerFactory.getLogger( Reporter.class );

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    final ProgressReporter reporter;

    public final String validatorName;

    final ValidatorResults.Builder results = ValidatorResults.create();

    public Reporter( final String validatorName, ProgressReporter reporter )
    {
        this.validatorName = validatorName;
        this.reporter = reporter;
    }

    public void addResult( final ValidatorResult result )
    {
        this.results.add( result );
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
            validatorName( validatorName ).
            build();

        try
        {
            final String value = OBJECT_MAPPER.writeValueAsString( description );
            reporter.info( value );
        }
        catch ( JsonProcessingException e )
        {
            LOG.error( "FAILED TO TRANSLATE TO JSON", e );
        }
    }

    public void reportProgress( final Long total, final Integer current )
    {
        reporter.progress( current, total.intValue() );
    }
}
