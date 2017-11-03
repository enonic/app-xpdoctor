package me.myklebust.xpdoctor.validator.nodevalidator.parentexists;

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

public class ParentExistsEntriesExecutor
    extends AbstractEntriesValidator
{
    public static final int BATCH_SIZE = 1_000;

    private final NodeService nodeService;

    private final Logger LOG = LoggerFactory.getLogger( ParentExistsEntriesExecutor.class );

    private final ParentExistsEntryExecutor entryExecutor;

    private ParentExistsEntriesExecutor( final Builder builder )
    {
        super( builder );
        nodeService = builder.nodeService;
        this.entryExecutor = ParentExistsEntryExecutor.create().
            nodeService( this.nodeService ).
            validatorName( validatorName ).
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

            results.add( checkNodes( executor.execute() ) );
            execute += BATCH_SIZE;
        }

        return results.build();
    }

    private List<ValidatorResult> checkNodes( final NodeIds nodeIds )
    {
        List<ValidatorResult> results = Lists.newArrayList();

        for ( final NodeId nodeId : nodeIds )
        {
            try
            {
                final ValidatorResult result = this.entryExecutor.execute( nodeId );

                if ( result != null )
                {
                    results.add( result );
                }
            }
            catch ( Exception e )
            {
                LOG.error( "Cannot check parent exists for node with id: " + nodeId + "", e );
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

        public ParentExistsEntriesExecutor build()
        {
            return new ParentExistsEntriesExecutor( this );
        }
    }
}
