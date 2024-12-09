package me.myklebust.xpdoctor.validator.nodevalidator;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.xp.node.FindNodesByQueryResult;
import com.enonic.xp.node.NodeIds;
import com.enonic.xp.node.NodeQuery;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.task.ProgressReporter;

public class BatchedQueryExecutor
{
    private static final Logger LOG = LoggerFactory.getLogger( BatchedQueryExecutor.class );

    private final int batchSize;

    private final NodeService nodeService;

    private int currentFrom = 0;

    private boolean hasMore = true;

    private final ProgressReporter progressReporter;

    private Long totalHits;

    private BatchedQueryExecutor( final Builder builder )
    {
        this.batchSize = 1_000;
        this.nodeService = builder.nodeService;
        this.progressReporter = builder.progressReporter;
        this.totalHits = -1L;
    }

    public void execute( Consumer<NodeIds> consumer )
    {
        while ( this.hasMore )
        {
            final NodeIds nodeIds = executeNext();
            LOG.info( "Checking nodes {} of {}", currentFrom, totalHits );
            progressReporter.progress( totalHits.intValue(), currentFrom );
            consumer.accept( nodeIds );
        }
    }

    public NodeIds executeNext()
    {
        final NodeQuery query = NodeQuery.create().from( this.currentFrom ).size( this.batchSize ).build();

        final FindNodesByQueryResult result = this.nodeService.findByQuery( query );
        totalHits = result.getTotalHits();

        if ( result.getNodeHits().isEmpty() )
        {
            this.hasMore = false;
        }
        else
        {
            this.currentFrom += this.batchSize;

            this.hasMore = currentFrom < result.getTotalHits();
        }
        return result.getNodeIds();
    }

    public static Builder create()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private NodeService nodeService;

        private ProgressReporter progressReporter;

        private Builder()
        {
        }

        public Builder nodeService( final NodeService val )
        {
            nodeService = val;
            return this;
        }

        public Builder progressReporter( final ProgressReporter val )
        {
            progressReporter = val;
            return this;
        }

        public BatchedQueryExecutor build()
        {
            return new BatchedQueryExecutor( this );
        }
    }
}
