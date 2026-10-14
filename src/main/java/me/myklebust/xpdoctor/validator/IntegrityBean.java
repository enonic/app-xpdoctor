package me.myklebust.xpdoctor.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.myklebust.xpdoctor.json.JsonMapGenerator;
import me.myklebust.xpdoctor.validator.mapper.RepoResultsMapper;
import me.myklebust.xpdoctor.validator.mapper.ValidatorsMapper;

import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.event.Event;
import com.enonic.xp.event.EventPublisher;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;
import com.enonic.xp.task.ProgressReporter;
import com.enonic.xp.task.RunnableTask;
import com.enonic.xp.task.SubmitLocalTaskParams;
import com.enonic.xp.task.TaskId;
import com.enonic.xp.task.TaskService;

@SuppressWarnings("unused")
public class IntegrityBean
    implements ScriptBean
{
    private static final Logger LOG = LoggerFactory.getLogger( IntegrityBean.class );

    public static final String ANALYSIS_TASK_DESCRIPTION = "com.enonic.app.xpdoctor";

    public static final String REPAIR_TASK_DESCRIPTION = "com.enonic.app.xpdoctor.repair";

    private ValidatorService validatorService;

    private TaskService taskService;

    private EventPublisher eventPublisher;

    private RepoValidationResults lastResult;

    private AtomicBoolean cancelRequested = new AtomicBoolean();

    private final AtomicBoolean operationInProgress = new AtomicBoolean( false );

    @Override
    public void initialize( final BeanContext context )
    {
        this.validatorService = context.getService( ValidatorService.class ).get();
        this.taskService = context.getService( TaskService.class ).get();
        this.eventPublisher = context.getService( EventPublisher.class ).get();
    }

    public Object getLastResult()
    {
        if ( lastResult != null )
        {
            return new RepoResultsMapper( this.lastResult );
        }

        return null;
    }

    @SuppressWarnings("unused")
    public Object validate( final ValidateParams params )
    {
        if ( !operationInProgress.compareAndSet( false, true ) )
        {
            throw new IllegalStateException( "An analysis or repair is already running" );
        }

        this.lastResult = null;

        final AtomicBoolean cancelled = new AtomicBoolean();
        this.cancelRequested = cancelled;

        final RunnableTask task = ( id, progressReporter ) -> {
            try
            {
                validatorTask( progressReporter, params, cancelled );
            }
            finally
            {
                operationInProgress.set( false );
            }
        };

        return submit( task, ANALYSIS_TASK_DESCRIPTION );
    }

    public void cancel()
    {
        this.cancelRequested.set( true );
    }

    public Object repairAll( final RepairAllParams params )
    {
        final RepoValidationResults result = this.lastResult;

        if ( result == null )
        {
            throw new IllegalStateException( "No analysis result to repair" );
        }

        final List<RepoValidationResults.Issue> issues = selectIssues( result, params );

        if ( !operationInProgress.compareAndSet( false, true ) )
        {
            throw new IllegalStateException( "An analysis or repair is already running" );
        }

        final AtomicBoolean cancelled = new AtomicBoolean();
        this.cancelRequested = cancelled;

        final RunnableTask task = ( id, progressReporter ) -> {
            try
            {
                repairTask( progressReporter, result, issues, params.isAll(), cancelled );
            }
            finally
            {
                operationInProgress.set( false );
            }
        };

        return submit( task, REPAIR_TASK_DESCRIPTION );
    }

    private String submit( final RunnableTask task, final String description )
    {
        try
        {
            return taskService.submitLocalTask( SubmitLocalTaskParams.create().runnableTask( task ).description( description ).build() )
                .toString();
        }
        catch ( RuntimeException e )
        {
            operationInProgress.set( false );
            throw e;
        }
    }

    private List<RepoValidationResults.Issue> selectIssues( final RepoValidationResults result, final RepairAllParams params )
    {
        if ( params.isAll() )
        {
            return result.getIssues()
                .values()
                .stream()
                .filter( issue -> issue.getResult().repairResult().status() == RepairStatus.IS_REPAIRABLE )
                .collect( Collectors.toList() );
        }

        final List<RepoValidationResults.Issue> issues = new ArrayList<>();
        for ( final String issueId : params.getIssueIds() )
        {
            final RepoValidationResults.Issue issue = result.getIssues().get( issueId );
            if ( issue != null )
            {
                issues.add( issue );
            }
        }
        return issues;
    }

    private void repairTask( final ProgressReporter progressReporter, final RepoValidationResults result,
                             final List<RepoValidationResults.Issue> issues, final boolean onlyRepairable,
                             final AtomicBoolean cancelled )
    {
        final int total = issues.size();
        int current = 0;

        for ( final RepoValidationResults.Issue issue : issues )
        {
            if ( cancelled.get() )
            {
                LOG.info( "Repair cancelled" );
                break;
            }

            progressReporter.info( "{\"nodeId\":\"" + issue.getResult().nodeId() + "\",\"current\":" + current + ",\"total\":" + total + "}" );
            progressReporter.progress( current++, total );

            if ( onlyRepairable && issue.getResult().repairResult().status() != RepairStatus.IS_REPAIRABLE )
            {
                continue;
            }

            final RepairResult repairResult = createContext( issue ).callWith( () -> doRepair( issue ) );
            issue.getResult().updateRepairResult( repairResult );
            result.bumpRepairVersion();
        }

        progressReporter.progress( total, total );
    }

    private RepairResult doRepair( final RepoValidationResults.Issue issue )
    {
        final ValidatorResult validatorResult = issue.getResult();
        final Validator validator = this.validatorService.getValidator( validatorResult.validatorName() );

        if ( validator == null )
        {
            return RepairResult.create()
                .repairStatus( RepairStatus.FAILED )
                .message( "Validator with name [" + validatorResult.validatorName() + "] not found" )
                .build();
        }

        try
        {
            return validator.repair( validatorResult.nodeId() );
        }
        catch ( Exception e )
        {
            LOG.error( "Repair of node [{}] failed", validatorResult.nodeId(), e );
            return RepairResult.create().repairStatus( RepairStatus.FAILED ).message( e.getMessage() ).build();
        }
    }

    private Context createContext( final RepoValidationResults.Issue issue )
    {
        return ContextBuilder.from( ContextAccessor.current() )
            .repositoryId( issue.getRepositoryId() )
            .branch( issue.getBranch() )
            .build();
    }

    public Object validators()
    {
        return new ValidatorsMapper( this.validatorService.getValidators() );
    }

    private void validatorTask( final ProgressReporter progressReporter, final ValidateParams params, final AtomicBoolean cancelled )
    {
        final RepoValidationResults result;
        try
        {
            result = this.validatorService.analyze( AnalyzeParams.create()
                                                        .progressReporter( progressReporter )
                                                        .enabledValidators( params.getEnabledValidators() )
                                                        .repoBranches( params.getRepoBranches() )
                                                        .cancelled( cancelled::get )
                                                        .build() );
        }
        catch ( AnalysisCancelledException e )
        {
            LOG.info( "Analysis cancelled" );
            return;
        }

        this.lastResult = result;

        final Object serializedResult = getSerializedResult( result );

        eventPublisher.publish(
            Event.create( "com.enonic.app.xpdoctor.jobFinished" ).distributed( true ).value( "result", serializedResult ).build() );
    }

    private Object getSerializedResult( final RepoValidationResults result )
    {
        final RepoResultsMapper resultsMapper = new RepoResultsMapper( result );
        final JsonMapGenerator jsonSerializer = new JsonMapGenerator();
        resultsMapper.serialize( jsonSerializer );
        return jsonSerializer.getRoot();
    }
}
