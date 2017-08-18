package me.myklebust.xpdoctor.validator;

import java.time.Instant;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.node.NodeVersionId;

public interface ValidatorResult
{
    RepairResult repairResult();

    NodeId nodeId();

    NodeVersionId nodeVersionId();

    NodePath nodePath();

    Instant timestamp();

    String message();

    String type();
}
