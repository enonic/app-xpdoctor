package me.myklebust.xpdoctor.validator;

import java.time.Instant;
import java.util.Set;

import me.myklebust.xpdoctor.validator.mapper.RepairResultMapper;
import me.myklebust.xpdoctor.validator.mapper.RepoResultsMapper;
import me.myklebust.xpdoctor.validator.mapper.ValidatorsMapper;

import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.event.Event;
import com.enonic.xp.event.EventPublisher;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;
import com.enonic.xp.script.serializer.JsonMapGenerator;
import com.enonic.xp.task.ProgressReporter;
import com.enonic.xp.task.RunnableTask;
import com.enonic.xp.task.TaskId;
import com.enonic.xp.task.TaskService;

@SuppressWarnings("unused")
public class IntegrityBean
    implements ScriptBean
{
    private ValidatorService validatorService;

    private TaskService taskService;

    private EventPublisher eventPublisher;

    private RepoValidationResults lastResult;

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
        this.lastResult = null;

        final RunnableTask task = ( id, progressReporter ) -> validatorTask( progressReporter, params );

        final TaskId taskId = taskService.submitTask( task, "com.enonic.app.xpdoctor" );

        return taskId.toString();
    }

    public Object repairAll()
    {
        return null;
    }

    public Object repair( final RepairParams params )
    {
        this.lastResult = null;

        return ContextBuilder.from( ContextAccessor.current() ).
            branch( params.getBranch() ).
            repositoryId( params.getRepoId() ).
            build().callWith( () -> doRepair( params ) );
    }

    private Object doRepair( final RepairParams params )
    {
        final Validator validator = this.validatorService.getValidator( params.getValidatorName() );

        if ( validator == null )
        {
            throw new IllegalArgumentException( "Validator with name [" + params.getValidatorName() + "] not found" );
        }

        final RepairResult repairResult = validator.repair( params.getNodeId() );

        return new RepairResultMapper( repairResult );
    }

    public Object validators()
    {
        final Set<Validator> validators = this.validatorService.getValidators();
        return new ValidatorsMapper( validators );
    }


    private void validatorTask( final ProgressReporter progressReporter, final ValidateParams params )
    {
        try
        {
            final RepoValidationResults result = this.validatorService.analyze( AnalyzeParams.create().
                progressReporter( progressReporter ).
                enabledValidators( params.getEnabledValidators() ).
                build() );

            this.lastResult = result;

            final Object serializedResult = getSerializedResult( result );

            eventPublisher.publish( Event.create( "com.enonic.app.xpdoctor.jobFinished" ).
                distributed( true ).
                localOrigin( true ).
                timestamp( Instant.now().toEpochMilli() ).
                value( "result", serializedResult ).
                build() );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            throw e;
        }

    }

    private Object getSerializedResult( final RepoValidationResults result )
    {
        final RepoResultsMapper resultsMapper = new RepoResultsMapper( result );
        final JsonMapGenerator jsonSerializer = new JsonMapGenerator();
        resultsMapper.serialize( jsonSerializer );
        return jsonSerializer.getRoot();
    }


}
