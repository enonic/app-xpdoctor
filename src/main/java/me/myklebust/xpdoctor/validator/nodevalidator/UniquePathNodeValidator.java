package me.myklebust.xpdoctor.validator.nodevalidator;

import me.myklebust.xpdoctor.validator.RepairOptions;
import me.myklebust.xpdoctor.validator.ValidationError;
import me.myklebust.xpdoctor.validator.ValidatorResult;

import com.enonic.xp.node.FindNodesByQueryResult;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeQuery;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.node.SearchMode;

public class UniquePathNodeValidator
    extends AbstractNodeValidator
{
    @Override
    public String name()
    {
        return "Unique path validator";
    }

    @Override
    public String getDescription()
    {
        return "Validates that a node has a unique path";
    }

    public UniquePathNodeValidator( final NodeService nodeService )
    {
        super( nodeService );
    }

    @Override
    public ValidatorResult validate( final Node node )
    {
        final FindNodesByQueryResult result = this.nodeService.findByQuery( NodeQuery.create().
            path( node.path() ).
            searchMode( SearchMode.COUNT ).
            build() );

        if ( result.getTotalHits() > 1 )
        {
            return new ValidatorResult( node.path(), node.id(), ValidationError.NOT_UNIQUE_PATH );
        }

        return null;
    }

    @Override
    public boolean repair( final NodeId nodeId, final RepairOptions options )
    {
        return false;
    }
}
