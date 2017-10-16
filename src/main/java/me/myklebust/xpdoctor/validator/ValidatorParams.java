package me.myklebust.xpdoctor.validator;

import com.enonic.xp.task.ProgressReporter;

public class ValidatorParams
{
    private final ProgressReporter progressReporter;

    public ValidatorParams( final ProgressReporter progressReporter )
    {
        this.progressReporter = progressReporter;
    }

    public ProgressReporter getProgressReporter()
    {
        return progressReporter;
    }
}
