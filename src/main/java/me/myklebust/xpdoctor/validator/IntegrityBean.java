package me.myklebust.xpdoctor.validator;

import java.time.Instant;
import java.util.Set;

import me.myklebust.xpdoctor.validator.mapper.RepoResultsMapper;
import me.myklebust.xpdoctor.validator.mapper.ValidatorsMapper;

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

    @Override
    public void initialize( final BeanContext context )
    {
        this.validatorService = context.getService( ValidatorService.class ).get();
        this.taskService = context.getService( TaskService.class ).get();
        this.eventPublisher = context.getService( EventPublisher.class ).get();
    }

    @SuppressWarnings("unused")
    public Object validate()
    {
        final RunnableTask task = ( id, progressReporter ) -> validatorTask( progressReporter );

        final TaskId taskId = taskService.submitTask( task, "com.enonic.app.xpdoctor" );

        return taskId.toString();
    }

    public Object validators()
    {
        final Set<Validator> validators = this.validatorService.getValidators();
        return new ValidatorsMapper( validators );
    }


    private void validatorTask( final ProgressReporter progressReporter )
    {
        try
        {
            final RepoValidationResults result = this.validatorService.execute( new ValidatorParams( progressReporter ) );

            final Object serializedResult = getSerializedResult( result );
            System.out.println( "Result: " + serializedResult );

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
