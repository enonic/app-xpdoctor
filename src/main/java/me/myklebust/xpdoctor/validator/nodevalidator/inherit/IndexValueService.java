package me.myklebust.xpdoctor.validator.nodevalidator.inherit;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Client;


import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.node.NodeId;

public class IndexValueService
{
    private final Client client;

    IndexValueService( final Client client )
    {
        this.client = client;
    }

    public Set<String> getFieldsValue( final NodeId nodeId, final String indexType, final Set<String> targetFields )
    {
        final String indexName = indexType + "-" + ContextAccessor.current().getRepositoryId();
        final String indexTypeName = ContextAccessor.current().getBranch().toString();

        final GetRequest getRequest = new GetRequest( indexName, indexTypeName, nodeId.toString() );

        final GetResponse getResponse = this.client.get( getRequest ).actionGet( 5000 );

        final List<LinkedHashSet<String>> results = getResponse.getSource()
            .entrySet()
            .stream()
            .filter( sourceEntry -> targetFields.contains( sourceEntry.getKey() ) )
            .map( sourceEntry -> new LinkedHashSet<>( (List<String>) sourceEntry.getValue() )

            )
            .filter( Objects::nonNull )
            .distinct()
            .collect( Collectors.toList() );

        if ( results.size() == 1 )
        {
            return results.get( 0 );
        }
        if ( results.size() > 1 )
        {
            throw new IllegalStateException( "requested indices have different values" );
        }

        return null;
    }

    public void update( final UpdateRequest updateRequest )
    {
        client.update( updateRequest ).actionGet( 5000 );
    }

}
