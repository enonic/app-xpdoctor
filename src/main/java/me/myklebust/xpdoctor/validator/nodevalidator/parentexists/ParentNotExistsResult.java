package me.myklebust.xpdoctor.validator.nodevalidator.parentexists;

import me.myklebust.xpdoctor.validator.ValidatorResult;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.script.serializer.MapGenerator;

public class ParentNotExistsResult
    implements ValidatorResult
{

    public static final String TYPE = "ParentNotExists";

    private final NodeId nodeId;

    private final NodePath nodePath;

    private ParentNotExistsResult( final Builder builder )
    {
        nodeId = builder.nodeId;
        nodePath = builder.nodePath;
    }

    public static Builder create()
    {
        return new Builder();
    }

    public NodeId getNodeId()
    {
        return nodeId;
    }

    public NodePath getNodePath()
    {
        return nodePath;
    }

    @Override
    public String type()
    {
        return TYPE;

    }

    public static final class Builder
    {
        private NodeId nodeId;

        private NodePath nodePath;

        private Builder()
        {
        }

        public Builder nodeId( final NodeId val )
        {
            nodeId = val;
            return this;
        }

        public Builder nodePath( final NodePath val )
        {
            nodePath = val;
            return this;
        }

        public ParentNotExistsResult build()
        {
            return new ParentNotExistsResult( this );
        }
    }

    @Override
    public void serialize( final MapGenerator gen )
    {
        gen.map();
        gen.value( "type", TYPE );
        gen.value( "nodePath", this.getNodePath() );
        gen.value( "id", this.getNodeId() );
        gen.end();
    }
}
