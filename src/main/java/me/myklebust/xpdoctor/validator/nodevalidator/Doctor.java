package me.myklebust.xpdoctor.validator.nodevalidator;

import me.myklebust.xpdoctor.validator.result.RepairResult;

import com.enonic.xp.node.NodeId;

public interface Doctor
{
    RepairResult repair( final NodeId nodeId );
}
