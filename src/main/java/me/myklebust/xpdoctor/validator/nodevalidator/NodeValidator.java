package me.myklebust.xpdoctor.validator.nodevalidator;

import me.myklebust.xpdoctor.validator.ValidatorResults;

import com.enonic.xp.node.NodeId;

public interface NodeValidator
{
    ValidatorResults validate();

    boolean repair( final NodeId nodeId );
}
