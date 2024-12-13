package me.myklebust.xpdoctor.validator;

import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetAction;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.index.IndexType;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeVersionId;
import com.enonic.xp.repository.RepositoryId;

@Component(immediate = true, service = StorageSpyService.class)
public class StorageSpyService
{
    @Reference
    private Client client;

    private final static String SEARCH_INDEX_PREFIX = "search";

    private final static String STORAGE_INDEX_PREFIX = "storage";

    private final static String DIVIDER = "-";

    public boolean existsInBranch( final NodeId nodeId, final RepositoryId repositoryId, final Branch branch )
    {
        final String indexName = getStorageIndexName( repositoryId );

        GetRequest getRequest = new GetRequestBuilder( client, GetAction.INSTANCE ).setIndex( indexName )
            .setRouting( nodeId.toString() )
            .setId( nodeId + "_" + branch.getValue() )
            .setType( IndexType.BRANCH.getName() )
            .request();

        final GetResponse getResponse = client.get( getRequest ).actionGet();

        return getResponse.isExists();
    }

    public GetResponse getInBranch( final NodeId nodeId, final RepositoryId repositoryId, final Branch branch )
    {
        final String indexName = getStorageIndexName( repositoryId );

        GetRequest getRequest = new GetRequestBuilder( client, GetAction.INSTANCE ).setIndex( indexName )
            .setRouting( nodeId.toString() )
            .setId( nodeId + "_" + branch.getValue() )
            .setType( IndexType.BRANCH.getName() )
            .request();

        final GetResponse getResponse = client.get( getRequest ).actionGet();

        return getResponse;
    }

    public SearchResponse findAllInBranch( final RepositoryId repositoryId, final Branch branch, final int size, final String fromNodeId )
    {
        final String indexName = getStorageIndexName( repositoryId );
        final BoolQueryBuilder query = QueryBuilders.boolQuery().must( QueryBuilders.termQuery( "branch", branch.getValue() ) );

        if ( fromNodeId != null )
        {
            query.must( QueryBuilders.rangeQuery( "nodeid" ).gt( fromNodeId ) );
        }

        return client.prepareSearch( indexName )
            .setTypes( IndexType.BRANCH.getName() )
            .addSort( "nodeid", SortOrder.ASC )
            .setQuery( query )
            .setSize( size )
            .execute()
            .actionGet();
    }

    public GetResponse getVersion( final NodeId nodeId, final NodeVersionId nodeVersionId, final RepositoryId repositoryId )
    {
        final String indexName = getStorageIndexName( repositoryId );

        GetRequest getRequest = new GetRequestBuilder( client, GetAction.INSTANCE ).setIndex( indexName )
            .setRouting( nodeId.toString() )
            .setId( nodeVersionId.toString() )
            .setType( IndexType.VERSION.getName() )
            .request();

        final GetResponse getResponse = client.get( getRequest ).actionGet();

        return getResponse;
    }

    public boolean existsInSearch( final NodeId nodeId, final RepositoryId repositoryId, final Branch branch )
    {
        final String indexName = getSearchIndexName( repositoryId );

        GetRequest getRequest = new GetRequest( indexName, branch.getValue(), nodeId.toString() );

        final GetResponse getResponse = client.get( getRequest ).actionGet();

        return getResponse.isExists();
    }

    public boolean deleteInSearch( final NodeId nodeId, final RepositoryId repositoryId, final Branch branch )
    {
        return deleteFromIndex( nodeId, IndexType.SEARCH, repositoryId, branch );
    }

    public Client getClient()
    {
        return client;
    }

    private boolean deleteFromIndex( final NodeId nodeId, final IndexType indexType, final RepositoryId repositoryId, final Branch branch )
    {

        if ( indexType.equals( IndexType.SEARCH ) )
        {
            final DeleteRequest deleteRequest =
                new DeleteRequest( getSearchIndexName( repositoryId ) ).type( branch.getValue() ).id( nodeId.toString() );

            System.out.println( "DELETE-REQUEST: " + deleteRequest );

            final DeleteResponse deleteResponse = client.delete( deleteRequest ).actionGet();

            System.out.println( "Delete-response: " + deleteResponse );

            return deleteResponse.isFound();
        }

        return false;
    }

    private String getStorageIndexName( final RepositoryId repositoryId )
    {
        return STORAGE_INDEX_PREFIX + DIVIDER + repositoryId;
    }

    private String getSearchIndexName( final RepositoryId repositoryId )
    {
        return SEARCH_INDEX_PREFIX + DIVIDER + repositoryId;
    }
}
