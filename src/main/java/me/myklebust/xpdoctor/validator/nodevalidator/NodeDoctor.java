package me.myklebust.xpdoctor.validator.nodevalidator;

import me.myklebust.xpdoctor.validator.RepairResult;

import com.enonic.xp.node.NodeId;

public interface NodeDoctor
{
    RepairResult repairNode( NodeId nodeId, boolean dryRun );
}
