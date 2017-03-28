package me.myklebust.xpdoctor.validator.nodevalidator;

import me.myklebust.xpdoctor.validator.Validator;

import com.enonic.xp.node.NodeService;

public abstract class AbstractNodeValidator
    implements Validator, NodeValidator
{
    protected NodeService nodeService;

    protected AbstractNodeValidator( final NodeService nodeService )
    {
        this.nodeService = nodeService;
    }
}
