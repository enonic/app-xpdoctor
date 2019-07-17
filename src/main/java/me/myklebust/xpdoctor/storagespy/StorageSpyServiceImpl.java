package me.myklebust.xpdoctor.storagespy;

import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.index.IndexType;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.repository.RepositoryId;

@Component(immediate = true)
public class StorageSpyServiceImpl
    implements StorageSpyService
{
    private Client client;

    private final static String SEARCH_INDEX_PREFIX = "search";

    private final static String STORAGE_INDEX_PREFIX = "storage";

    private final static String DIVIDER = "-";


    @Override
    public boolean existsInBranch( final NodeId nodeId, final RepositoryId repositoryId, final Branch branch )
    {
        final String indexName = getStorageIndexName( repositoryId );

        GetRequest getRequest = new GetRequestBuilder( client ).
            setIndex( indexName ).
            setRouting( nodeId.toString() ).
            setId( nodeId + "_" + branch.getValue() ).
            setType( IndexType.BRANCH.getName() ).request();

        final GetResponse getResponse = client.get( getRequest ).actionGet();

        return getResponse.isExists();
    }

    @Override
    public boolean existsInSearch( final NodeId nodeId, final RepositoryId repositoryId, final Branch branch )
    {
        final String indexName = getSearchIndexName( repositoryId );

        GetRequest getRequest = new GetRequest( indexName, branch.getValue(), nodeId.toString() );

        final GetResponse getResponse = client.get( getRequest ).actionGet();

        return getResponse.isExists();
    }

    @Override
    public boolean deleteInSearch( final NodeId nodeId, final RepositoryId repositoryId, final Branch branch )
    {
        return deleteFromIndex( nodeId, IndexType.SEARCH, repositoryId, branch );
    }

    private boolean deleteFromIndex( final NodeId nodeId, final IndexType indexType, final RepositoryId repositoryId, final Branch branch )
    {

        if ( indexType.equals( IndexType.SEARCH ) )
        {
            final DeleteRequest deleteRequest = new DeleteRequest( getSearchIndexName( repositoryId ) ).
                type( branch.getValue() ).
                id( nodeId.toString() );

            System.out.println( "DELETE-REQUEST: " + deleteRequest );

            final DeleteResponse deleteResponse = client.delete( deleteRequest ).actionGet();

            System.out.println( "Delete-response: " + deleteResponse );

            return deleteResponse.isFound();
        }

        return false;
    }

    private String getStorageIndexName( final RepositoryId repositoryId )
    {
        return STORAGE_INDEX_PREFIX + DIVIDER + repositoryId.toString();
    }

    private String getSearchIndexName( final RepositoryId repositoryId )
    {
        return SEARCH_INDEX_PREFIX + DIVIDER + repositoryId.toString();
    }

    public Client getClient()
    {
        return client;
    }

    @Reference
    public void setClient( final Client client )
    {
        this.client = client;
    }
}
