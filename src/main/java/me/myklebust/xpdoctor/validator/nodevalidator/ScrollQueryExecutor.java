package me.myklebust.xpdoctor.validator.nodevalidator;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;

import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.myklebust.xpdoctor.validator.StorageSpyService;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeIds;
import com.enonic.xp.repository.RepositoryId;
import com.enonic.xp.task.ProgressReporter;

public class ScrollQueryExecutor
{
    public enum IndexType
    {
        STORAGE, SEARCH
    }

    private static final Logger LOG = LoggerFactory.getLogger( ScrollQueryExecutor.class );

    private final int batchSize;

    private final Client client;

    private final ProgressReporter progressReporter;

    private final String indexName;

    private final String types;

    private final QueryBuilder query;

    private Function<SearchHit, NodeId> idMapping;

    private String scrollId;

    private ScrollQueryExecutor( final Builder builder )
    {
        this.batchSize = 1_000;
        this.client = builder.storageSpyService.getClient();
        this.progressReporter = builder.progressReporter;

        RepositoryId repositoryId = ContextAccessor.current().getRepositoryId();
        Branch branch = ContextAccessor.current().getBranch();

        IndexType indexType = builder.indexType;
        this.indexName = ( indexType == IndexType.SEARCH ? "search-" : "storage-" ) + repositoryId;
        this.types = indexType == IndexType.SEARCH ? branch.getValue() : "branch";
        this.query = indexType == IndexType.SEARCH ? QueryBuilders.matchAllQuery() : QueryBuilders.termQuery( "branch", branch.getValue() );

        this.idMapping = hit -> indexType == IndexType.SEARCH
            ? NodeId.from( hit.getId() )
            : NodeId.from( hit.getId().substring( 0, hit.getId().lastIndexOf( '_' ) ) );
    }

    public void execute( final Consumer<NodeIds> consumer )
    {
        int totalHits = getTotal();

        try
        {
            int currentFrom = 0;
            while ( true )
            {
                final SearchResponse allInSearch = findAll();
                scrollId = allInSearch.getScrollId();

                final NodeIds nodeIds = NodeIds.from(
                    Arrays.stream( allInSearch.getHits().getHits() ).map( idMapping ).toArray( NodeId[]::new ) );
                if ( nodeIds.isEmpty() )
                {
                    return;
                }
                else
                {
                    LOG.info( "Checking nodes {}-{} of {}", currentFrom, Math.min( currentFrom - 1 + batchSize, totalHits ), totalHits );
                    progressReporter.progress( totalHits, currentFrom );
                    currentFrom += nodeIds.getSize();

                    consumer.accept( nodeIds );
                }
            }
        }
        finally
        {
            if ( scrollId != null )
            {
                clearScroll( scrollId );
            }
        }
    }

    public static Builder create()
    {
        return new Builder();
    }

    private int getTotal()
    {
        return (int) client.prepareSearch( indexName )
            .setTypes( types )
            .setSize( 0 )
            .setQuery( query )
            .execute()
            .actionGet()
            .getHits()
            .getTotalHits();
    }

    private SearchResponse findAll()
    {
        if ( scrollId != null )
        {
            return client.prepareSearchScroll( scrollId ).setScroll( TimeValue.timeValueHours( 24 ) ).execute().actionGet();
        }
        else
        {
            return client.prepareSearch( indexName )
                .setTypes( types )
                .setSize( batchSize )
                .setQuery( query )
                .setFetchSource( false )
                .addSort( "_doc", SortOrder.ASC )
                .setScroll( TimeValue.timeValueHours( 24 ) )
                .execute()
                .actionGet();
        }
    }

    public void clearScroll( final String scrollId )
    {
        final ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        clearScrollRequest.addScrollId( scrollId );

        client.clearScroll( clearScrollRequest ).actionGet();
    }

    public static final class Builder
    {
        private StorageSpyService storageSpyService;

        private ProgressReporter progressReporter;

        private IndexType indexType = IndexType.SEARCH;

        private Builder()
        {
        }

        public Builder indexType( final IndexType val )
        {
            indexType = val;
            return this;
        }

        public Builder spyStorageService( final StorageSpyService val )
        {
            storageSpyService = val;
            return this;
        }

        public Builder progressReporter( final ProgressReporter val )
        {
            progressReporter = val;
            return this;
        }

        public ScrollQueryExecutor build()
        {
            return new ScrollQueryExecutor( this );
        }
    }
}
