package me.myklebust.xpdoctor.validator.nodevalidator;

import me.myklebust.xpdoctor.validator.RepairOptions;
import me.myklebust.xpdoctor.validator.ValidationError;
import me.myklebust.xpdoctor.validator.ValidatorResult;

import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeService;

public class ParentExistsNodeValidator
    extends AbstractNodeValidator
{
    @Override
    public String name()
    {
        return "Parent exists validator";
    }

    @Override
    public String getDescription()
    {
        return "Validates that a node has a valid parent";
    }

    public ParentExistsNodeValidator( final NodeService nodeService )
    {
        super( nodeService );
    }

    @Override
    public ValidatorResult validate( final Node node )
    {
        if ( node.isRoot() )
        {
            return null;
        }

        final Node parent = this.nodeService.getByPath( node.parentPath() );

        if ( parent == null )
        {
            return new ValidatorResult( node.path(), node.id(), ValidationError.PARENT_MISSING );
        }

        return null;
    }

    @Override
    public boolean repair( final NodeId nodeId, final RepairOptions options )
    {
        return false;
    }

}
