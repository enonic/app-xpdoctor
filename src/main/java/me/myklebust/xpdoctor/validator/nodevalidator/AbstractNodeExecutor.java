package me.myklebust.xpdoctor.validator.nodevalidator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.task.ProgressReporter;

public abstract class AbstractNodeExecutor
{
    private final ProgressReporter reporter;

    protected final String validatorName;

    private final ObjectMapper objectMapper;

    protected AbstractNodeExecutor( final Builder builder )
    {
        this.validatorName = builder.validatorName;
        this.reporter = builder.reporter;
        this.objectMapper = new ObjectMapper();
    }

    protected void reportStart()
    {
        System.out.println( "------ Reporting start" );

        final Context context = ContextAccessor.current();

        final ProgressDescription description = ProgressDescription.create().
            branch( context.getBranch() ).
            repositoryId( context.getRepositoryId() ).
            validatorName( validatorName ).
            build();

        try
        {
            final String value = objectMapper.writeValueAsString( description );
            reporter.info( value );
        }
        catch ( JsonProcessingException e )
        {
            System.out.println( "FAILED TO TRANSLATE TO JSON:" + e.toString() );
        }
    }

    protected void reportProgress( final Long total, final Long current )
    {
        reporter.progress( current.intValue(), total.intValue() );
    }

    protected void reportProgress( final Long total, final Integer current )
    {
        reporter.progress( current, total.intValue() );
    }

    protected static class Builder<B extends Builder>
    {
        private ProgressReporter reporter;

        private String validatorName;

        @SuppressWarnings("unchecked")
        public B validatorName( final String validatorName )
        {
            this.validatorName = validatorName;
            return (B) this;
        }


        @SuppressWarnings("unchecked")
        public B progressReporter( final ProgressReporter reporter )
        {
            this.reporter = reporter;
            return (B) this;
        }


    }
}
