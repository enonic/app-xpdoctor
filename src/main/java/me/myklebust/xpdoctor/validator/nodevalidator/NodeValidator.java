package me.myklebust.xpdoctor.validator.nodevalidator;

import me.myklebust.xpdoctor.validator.RepairOptions;
import me.myklebust.xpdoctor.validator.ValidatorResult;

import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;

public interface NodeValidator
{
    ValidatorResult validate( final Node node );

    boolean repair( final NodeId nodeId, final RepairOptions options );
}
