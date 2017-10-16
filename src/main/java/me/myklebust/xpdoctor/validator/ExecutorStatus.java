package me.myklebust.xpdoctor.validator;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.repository.RepositoryId;

public class ExecutorStatus
{
    private Validator runningTask;

    private RepositoryId currentRepo;

    private Branch currentBranch;

    public ExecutorStatus()
    {

    }

    public void setRunningTask( final Validator runningTask )
    {
        this.runningTask = runningTask;
    }

    public Validator getRunningTask()
    {
        return runningTask;
    }

    public RepositoryId getCurrentRepo()
    {
        return currentRepo;
    }

    public void setCurrentRepo( final RepositoryId currentRepo )
    {
        this.currentRepo = currentRepo;
    }

    public Branch getCurrentBranch()
    {
        return currentBranch;
    }

    public void setCurrentBranch( final Branch currentBranch )
    {
        this.currentBranch = currentBranch;
    }
}
