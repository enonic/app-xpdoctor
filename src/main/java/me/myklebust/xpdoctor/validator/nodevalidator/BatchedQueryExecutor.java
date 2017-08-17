package me.myklebust.xpdoctor.validator.nodevalidator;

import com.enonic.xp.node.FindNodesByQueryResult;
import com.enonic.xp.node.NodeIds;
import com.enonic.xp.node.NodeQuery;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.query.expr.OrderExpressions;
import com.enonic.xp.query.filter.Filters;

public class BatchedQueryExecutor
{
    private final int batchSize;

    private final NodeService nodeService;

    private int currentFrom = 0;

    private boolean hasMore = true;

    private final Filters filters;

    private final OrderExpressions orderBy;

    private final Long totalHits;

    private BatchedQueryExecutor( final Builder builder )
    {
        this.batchSize = builder.batchSize;
        this.nodeService = builder.nodeService;
        this.filters = builder.filters;
        this.orderBy = builder.orderBy;
        this.totalHits = initTotalHits();
    }

    private long initTotalHits()
    {
        final NodeQuery query = createQuery( 0, 0 );

        final FindNodesByQueryResult result = this.nodeService.findByQuery( query );

        return result.getTotalHits();
    }

    public NodeIds execute()
    {
        final NodeQuery query = createQuery( this.currentFrom, this.batchSize );

        final FindNodesByQueryResult result = this.nodeService.findByQuery( query );

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

    public static final class Builder
    {
        private int batchSize = 1000;

        private NodeService nodeService;

        private Filters filters;

        private OrderExpressions orderBy;

        private Builder()
        {
        }

        public Builder batchSize( final int val )
        {
            batchSize = val;
            return this;
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

        public BatchedQueryExecutor build()
        {
            return new BatchedQueryExecutor( this );
        }
    }


}
