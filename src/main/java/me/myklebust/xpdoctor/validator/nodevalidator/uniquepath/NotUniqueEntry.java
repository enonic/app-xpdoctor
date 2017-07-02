package me.myklebust.xpdoctor.validator.nodevalidator.uniquepath;

import java.time.Instant;

import com.enonic.xp.branch.Branches;
import com.enonic.xp.node.NodeId;

public class NotUniqueEntry
{
    private final NodeId nodeId;

    private final Instant instant;

    private final Branches branches;

    private NotUniqueEntry( final Builder builder )
    {
        nodeId = builder.nodeId;
        instant = builder.instant;
        branches = builder.branches;
    }

    public NodeId getNodeId()
    {
        return nodeId;
    }

    public Instant getInstant()
    {
        return instant;
    }

    public Branches getBranches()
    {
        return branches;
    }

    public static Builder create()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private NodeId nodeId;

        private Instant instant;

        private Branches branches;

        private Builder()
        {
        }

        public Builder nodeId( final NodeId val )
        {
            nodeId = val;
            return this;
        }

        public Builder instant( final Instant val )
        {
            instant = val;
            return this;
        }

        public Builder branches( final Branches val )
        {
            branches = val;
            return this;
        }

        public NotUniqueEntry build()
        {
            return new NotUniqueEntry( this );
        }
    }
}