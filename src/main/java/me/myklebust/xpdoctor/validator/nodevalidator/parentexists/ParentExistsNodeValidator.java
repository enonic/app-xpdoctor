package me.myklebust.xpdoctor.validator.nodevalidator.parentexists;

import me.myklebust.xpdoctor.validator.ValidatorResults;
import me.myklebust.xpdoctor.validator.nodevalidator.AbstractNodeValidator;

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
    public ValidatorResults validate()
    {
        return new ParentExistsExistsExecutor( this.nodeService ).execute();
    }

    @Override
    public boolean repair( final NodeId nodeId )
    {
        return false;
    }

}
