package me.myklebust.xpdoctor.validator.nodevalidator.unloadable;

import me.myklebust.xpdoctor.validator.ValidatorResults;
import me.myklebust.xpdoctor.validator.nodevalidator.AbstractNodeValidator;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeService;

public class LoadableNodeValidator
    extends AbstractNodeValidator
{

    @Override
    public String name()
    {
        return "Lodable";
    }

    @Override
    public String getDescription()
    {
        return "Validates that a node is loadable";
    }

    public LoadableNodeValidator( final NodeService nodeService )
    {
        super( nodeService );
    }

    @Override
    public ValidatorResults validate()
    {
        return new LoadableNodeExecutor( this.nodeService ).execute();
    }

    @Override
    public boolean repair( final NodeId nodeId )
    {
        return false;
    }
}
