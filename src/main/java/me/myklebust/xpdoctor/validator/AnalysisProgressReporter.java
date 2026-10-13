package me.myklebust.xpdoctor.validator;

import me.myklebust.xpdoctor.validator.nodevalidator.ProgressDescription;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.repository.RepositoryId;
import com.enonic.xp.task.ProgressReportParams;
import com.enonic.xp.task.ProgressReporter;

public class AnalysisProgressReporter
    implements ProgressReporter
{
    private final ProgressReporter delegate;

    private final int grandTotal;

    private int completed = 0;

    private int validatorTotal = 0;

    private RepositoryId repositoryId;

    private Branch branch;

    private String validator;

    public AnalysisProgressReporter( final ProgressReporter delegate, final int grandTotal )
    {
        this.delegate = delegate;
        this.grandTotal = grandTotal;
    }

    public void startValidator( final RepositoryId repositoryId, final Branch branch, final String validator, final int nodeCount )
    {
        this.repositoryId = repositoryId;
        this.branch = branch;
        this.validator = validator;
        this.validatorTotal = nodeCount;
        report( 0, nodeCount );
    }

    public void validatorDone()
    {
        this.completed += this.validatorTotal;
        report( this.validatorTotal, this.validatorTotal );
    }

    public void done()
    {
        delegate.progress( grandTotal, grandTotal );
    }

    @Override
    public void progress( final ProgressReportParams params )
    {
        progress( params.getCurrent() == null ? 0 : params.getCurrent(), params.getTotal() == null ? 0 : params.getTotal() );
    }

    @Override
    public void progress( final int current, final int total )
    {
        report( current, total );
    }

    @Override
    public void info( final String info )
    {
        delegate.info( info );
    }

    private void report( final int current, final int total )
    {
        final int overall = Math.min( completed + Math.min( current, validatorTotal ), grandTotal );
        delegate.info( ProgressDescription.create()
                           .repositoryId( repositoryId )
                           .branch( branch )
                           .validator( validator )
                           .current( current )
                           .total( total )
                           .build()
                           .toJson() );
        delegate.progress( overall, grandTotal );
    }
}
