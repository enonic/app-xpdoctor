package me.myklebust.xpdoctor.validator.nodevalidator.unloadable;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import me.myklebust.xpdoctor.validator.nodevalidator.AbstractEntriesValidator;
import me.myklebust.xpdoctor.validator.nodevalidator.BatchedQueryExecutor;
import me.myklebust.xpdoctor.validator.result.ValidatorResult;
import me.myklebust.xpdoctor.validator.result.ValidatorResults;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeIds;
import com.enonic.xp.node.NodeService;

public class LoadableEntriesValidator
    extends AbstractEntriesValidator
{
    private final static String TYPE = "Unloadable node";

    public static final int BATCH_SIZE = 1_000;

    private final NodeService nodeService;

    private final LoadableNodeDoctor doctor;

    private Logger LOG = LoggerFactory.getLogger( LoadableEntriesValidator.class );

    private final LoadableEntryValidator entryValidator;

    private LoadableEntriesValidator( final Builder builder )
    {
        super( builder );
        nodeService = builder.nodeService;
        this.doctor = new LoadableNodeDoctor( this.nodeService );
        this.entryValidator = LoadableEntryValidator.create().
            validatorName( validatorName ).
            doctor( this.doctor ).
            nodeService( this.nodeService ).
            build();
    }


    public static Builder create()
    {
        return new Builder();
    }

    public ValidatorResults execute()
    {
        LOG.info( "Running LoadableNodeExecutor..." );

        reportStart();

        final BatchedQueryExecutor executor = BatchedQueryExecutor.create().
            batchSize( BATCH_SIZE ).
            nodeService( this.nodeService ).
            build();

        final ValidatorResults.Builder results = ValidatorResults.create();

        int execute = 0;

        while ( executor.hasMore() )
        {
            LOG.info( "Checking nodes " + execute + "->" + ( execute + BATCH_SIZE ) + " of " + executor.getTotalHits() );
            reportProgress( executor.getTotalHits(), execute );

            final NodeIds nodesToCheck = executor.execute();
            results.add( checkNodes( nodesToCheck, false ) );
            execute += BATCH_SIZE;
        }

        LOG.info( ".... LoadableNodeExecutor done" );

        return results.build();
    }

    private List<ValidatorResult> checkNodes( final NodeIds nodeIds, final boolean repair )
    {
        List<ValidatorResult> results = Lists.newArrayList();

        for ( final NodeId nodeId : nodeIds )
        {
            final ValidatorResult result = this.entryValidator.execute( nodeId );
            if ( result != null )
            {
                results.add( result );
            }
        }
        return results;
    }

    public static final class Builder
        extends AbstractEntriesValidator.Builder<Builder>
    {
        private NodeService nodeService;

        private Builder()
        {
        }

        public Builder nodeService( final NodeService val )
        {
            nodeService = val;
            return this;
        }

        public LoadableEntriesValidator build()
        {
            return new LoadableEntriesValidator( this );
        }
    }
}
