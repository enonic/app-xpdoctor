package me.myklebust.xpdoctor.validator;

import java.time.Instant;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.node.NodeVersionId;

public class ValidatorResult
{
    private final NodeId nodeId;

    private final NodeVersionId nodeVersionId;

    private final NodePath nodePath;

    private final Instant timestamp;

    private final String type;

    private final RepairResult repairResult;

    private final String message;

    private final String validatorName;

    private ValidatorResult( final Builder builder )
    {
        nodeId = builder.nodeId;
        nodeVersionId = builder.nodeVersionId;
        nodePath = builder.nodePath;
        timestamp = builder.timestamp;
        type = builder.type;
        repairResult = builder.repairResult;
        message = builder.message;
        validatorName = builder.validatorName;
    }

    public RepairResult repairResult()
    {
        return this.repairResult;
    }

    public NodeId nodeId()
    {
        return this.nodeId;
    }

    public NodeVersionId nodeVersionId()
    {
        return this.nodeVersionId;
    }

    public NodePath nodePath()
    {
        return this.nodePath;
    }

    public Instant timestamp()
    {
        return this.timestamp;
    }

    public String message()
    {
        return this.message;
    }

    public String type()
    {
        return this.type;
    }

    public String validatorName()
    {
        return this.validatorName;
    }

    public static Builder create()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private NodeId nodeId;

        private NodeVersionId nodeVersionId;

        private NodePath nodePath;

        private Instant timestamp;

        private String type;

        private RepairResult repairResult;

        private String message;

        private String validatorName;

        private Builder()
        {
        }

        public Builder nodeId( final NodeId val )
        {
            nodeId = val;
            return this;
        }

        public Builder nodeVersionId( final NodeVersionId val )
        {
            nodeVersionId = val;
            return this;
        }

        public Builder nodePath( final NodePath val )
        {
            nodePath = val;
            return this;
        }

        public NodePath nodePath()
        {
            return nodePath;
        }

        public Builder timestamp( final Instant val )
        {
            timestamp = val;
            return this;
        }

        public Builder type( final String val )
        {
            type = val;
            return this;
        }

        public Builder repairResult( final RepairResult val )
        {
            repairResult = val;
            return this;
        }

        public Builder message( final String message )
        {
            this.message = message;
            return this;
        }

        public Builder validatorName( final String val )
        {
            validatorName = val;
            return this;
        }

        public ValidatorResult build()
        {
            return new ValidatorResult( this );
        }
    }
}
