package me.myklebust.xpdoctor.validator.nodevalidator;

import me.myklebust.xpdoctor.validator.ValidatorResult;
import me.myklebust.xpdoctor.validator.ValidatorResults;

import com.enonic.xp.task.ProgressReporter;

public class Reporter
{
    private static final long REPORT_INTERVAL_MS = 200;

    final ProgressReporter progressReporter;

    public final String validatorName;

    final ValidatorResults.Builder results = ValidatorResults.create();

    private int batchFrom;

    private int total;

    private int checkedInBatch;

    private long lastReport;

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

    public void startBatch( final int from, final int total )
    {
        this.batchFrom = from;
        this.total = total;
        this.checkedInBatch = 0;
        this.lastReport = System.currentTimeMillis();
        progressReporter.progress( from, total );
    }

    public void nodeChecked()
    {
        this.checkedInBatch++;
        final long now = System.currentTimeMillis();
        if ( now - this.lastReport >= REPORT_INTERVAL_MS )
        {
            this.lastReport = now;
            progressReporter.progress( this.batchFrom + this.checkedInBatch, this.total );
        }
    }

    public ProgressReporter getProgressReporter()
    {
        return progressReporter;
    }
}
