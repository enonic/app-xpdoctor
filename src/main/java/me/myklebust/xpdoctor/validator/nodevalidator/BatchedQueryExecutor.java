package me.myklebust.xpdoctor.validator.nodevalidator;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.myklebust.xpdoctor.validator.nodevalidator.blobmissing.BlobMissingExecutor;

import com.enonic.xp.node.FindNodesByQueryResult;
import com.enonic.xp.node.NodeIds;
import com.enonic.xp.node.NodeQuery;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.query.expr.OrderExpressions;
import com.enonic.xp.query.filter.Filters;
import com.enonic.xp.task.ProgressReporter;

public class BatchedQueryExecutor
{
    private static final Logger LOG = LoggerFactory.getLogger( BatchedQueryExecutor.class );

    private final int batchSize;

    private final NodeService nodeService;

    private int currentFrom = 0;

    private boolean hasMore = true;

    private final Filters filters;

    private final OrderExpressions orderBy;

    private final ProgressReporter progressReporter;

    private Long totalHits;

    private BatchedQueryExecutor( final Builder builder )
    {
        this.batchSize = builder.batchSize;
        this.nodeService = builder.nodeService;
        this.filters = builder.filters;
        this.orderBy = builder.orderBy;
        this.progressReporter = builder.progressReporter;
        this.totalHits = -1L;
    }

    public NodeIds execute()
    {
        NodeQuery query = createQuery( this.currentFrom, this.batchSize );

        FindNodesByQueryResult result = this.nodeService.findByQuery( query );
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

    private NodeQuery createQuery( final int from, final int size )
    {
        final NodeQuery.Builder query = NodeQuery.create().
            from( from ).
            size( size );

        if ( this.filters != null )
        {
            query.addQueryFilters( this.filters );
        }
        if ( this.orderBy != null )
        {
            query.setOrderExpressions( this.orderBy );
        }

        return query.build();
    }

    public Long getTotalHits()
    {
        return totalHits;
    }

    public boolean hasMore()
    {
        return this.hasMore;
    }

    public static Builder create()
    {
        return new Builder();
    }

    public void nextBatch( Consumer<NodeIds> consumer )
    {
        final NodeIds nodeIds = execute();
        LOG.info( "Checking nodes {} of {}", currentFrom, totalHits );
        progressReporter.progress( totalHits.intValue(), currentFrom );
        consumer.accept( nodeIds );
    }

    public static final class Builder
    {
        private int batchSize = 1_000;

        private NodeService nodeService;

        private Filters filters;

        private OrderExpressions orderBy;

        private ProgressReporter progressReporter;

        private Builder()
        {
        }

        public Builder nodeService( final NodeService val )
        {
            nodeService = val;
            return this;
        }

        public Builder filters( final Filters val )
        {
            filters = val;
            return this;
        }

        public Builder orderBy( final OrderExpressions val )
        {
            orderBy = val;
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
