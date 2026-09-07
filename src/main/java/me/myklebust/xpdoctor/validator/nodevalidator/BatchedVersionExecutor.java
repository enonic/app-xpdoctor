package me.myklebust.xpdoctor.validator.nodevalidator;

import com.enonic.xp.index.IndexPath;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.node.NodeVersionQuery;
import com.enonic.xp.node.NodeVersionQueryResult;
import com.enonic.xp.node.NodeVersions;
import com.enonic.xp.query.expr.FieldOrderExpr;
import com.enonic.xp.query.expr.OrderExpr;

public class BatchedVersionExecutor
{
    private final int batchSize;

    private final NodeService nodeService;

    private int currentFrom = 0;

    private boolean hasMore = true;

    private final Long totalHits;

    private final NodeId nodeId;

    private BatchedVersionExecutor( final Builder builder )
    {
        this.batchSize = builder.batchSize;
        this.nodeService = builder.nodeService;
        this.nodeId = builder.nodeId;
        this.totalHits = initTotalHits();
    }

    public NodeVersions execute()
    {
        final NodeVersionQueryResult result = this.nodeService.findVersions( NodeVersionQuery.create().
            nodeId( this.nodeId ).
            from( this.currentFrom ).
            size( this.batchSize ).
            addOrderBy( FieldOrderExpr.create( IndexPath.from( "timestamp" ), OrderExpr.Direction.DESC ) ).
            build() );

        if ( result.getNodeVersions().isEmpty() )
        {
            this.hasMore = false;
        }
        else
        {
            this.currentFrom += this.batchSize;

            this.hasMore = currentFrom < result.getTotalHits();
        }
        return result.getNodeVersions();
    }

    public boolean hasMore()
    {
        return hasMore;
    }

    public Long getTotalHits()
    {
        return totalHits;
    }

    private long initTotalHits()
    {
        final NodeVersionQueryResult result = this.nodeService.findVersions( NodeVersionQuery.create().
            nodeId( this.nodeId ).
            from( 0 ).
            size( 0 ).
            build() );

        return result.getTotalHits();
    }

    public static Builder create( final NodeService nodeService )
    {
        return new Builder( nodeService );
    }

    public static final class Builder
    {
        private int batchSize = 1000;

        private NodeService nodeService;

        private NodeId nodeId;

        private Builder( final NodeService nodeService )
        {
            this.nodeService = nodeService;
        }

        public Builder batchSize( final int val )
        {
            batchSize = val;
            return this;
        }

        public Builder nodeId( final NodeId nodeId )
        {
            this.nodeId = nodeId;
            return this;
        }

        public BatchedVersionExecutor build()
        {
            return new BatchedVersionExecutor( this );
        }
    }
}
