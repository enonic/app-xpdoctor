package me.myklebust.xpdoctor.validator.nodevalidator;

import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.task.ProgressReporter;

public abstract class AbstractNodeExecutor
{
    private final ProgressReporter reporter;

    public AbstractNodeExecutor( final ProgressReporter reporter )
    {
        this.reporter = reporter;
    }

    protected void reportStart()
    {
        final Context context = ContextAccessor.current();
        reporter.info( String.format( "Validator: %s - Context: [%s:%s]", this.getClass().getSimpleName(), context.getRepositoryId(),
                                      context.getBranch() ) );
    }

    protected void reportProgress( final Long total, final Long current )
    {
        reporter.progress( current.intValue(), total.intValue() );
    }

    protected void reportProgress( final Long total, final Integer current )
    {
        reporter.progress( current, total.intValue() );
    }
}
