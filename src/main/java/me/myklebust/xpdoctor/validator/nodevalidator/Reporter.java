package me.myklebust.xpdoctor.validator.nodevalidator;

import me.myklebust.xpdoctor.validator.ValidatorResult;
import me.myklebust.xpdoctor.validator.ValidatorResults;

import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.task.ProgressReporter;

public class Reporter
{
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

        progressReporter.info( toJson( description ) );
    }

    private static String toJson( final ProgressDescription description )
    {
        return "{\"repositoryId\":\"" + escapeJson( description.getRepositoryId() ) + "\"" +
            ",\"branch\":\"" + escapeJson( description.getBranch() ) + "\"" +
            ",\"validator\":\"" + escapeJson( description.getValidator() ) + "\"}";
    }

    private static String escapeJson( final String value )
    {
        if ( value == null )
        {
            return "";
        }
        return value.replace( "\\", "\\\\" ).replace( "\"", "\\\"" ).replace( "\n", "\\n" ).replace( "\r", "\\r" ).replace( "\t", "\\t" );
    }

    public ProgressReporter getProgressReporter()
    {
        return progressReporter;
    }
}
