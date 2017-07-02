package me.myklebust.xpdoctor.validator.nodevalidator.unloadable;

import me.myklebust.xpdoctor.validator.ValidatorResult;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.script.serializer.MapGenerator;

public class UnloadableNodeResult
    implements ValidatorResult
{

    public static final String UNLOADABLE = "Unloadable";

    private final NodeId nodeId;

    private final Exception exception;

    private UnloadableNodeResult( final Builder builder )
    {
        exception = builder.exception;
        nodeId = builder.nodeId;
    }

    public static Builder create()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private Exception exception;

        private NodeId nodeId;

        private Builder()
        {
        }

        public Builder exception( final Exception val )
        {
            exception = val;
            return this;
        }

        public Builder nodeId( final NodeId val )
        {
            nodeId = val;
            return this;
        }

        public UnloadableNodeResult build()
        {
            return new UnloadableNodeResult( this );
        }
    }

    @Override
    public String type()
    {
        return UNLOADABLE;
    }

    @Override
    public void serialize( final MapGenerator gen )
    {
        gen.map();
        gen.value( "type", UNLOADABLE );
        gen.value( "id", this.nodeId );
        gen.value( "exception", this.exception.getMessage() );
        gen.end();
    }


}
