package me.myklebust.xpdoctor.validator;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodePath;

public class ValidatorResult
{
    private final NodePath path;

    private final NodeId nodeId;

    private final ValidationError error;

    public ValidatorResult( final NodePath path, final NodeId nodeId, final ValidationError error )
    {
        this.path = path;
        this.error = error;
        this.nodeId = nodeId;
    }

    public NodeId getNodeId()
    {
        return nodeId;
    }

    public NodePath getPath()
    {
        return path;
    }

    public ValidationError getError()
    {
        return error;
    }
}

