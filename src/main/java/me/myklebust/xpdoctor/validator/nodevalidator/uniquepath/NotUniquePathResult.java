package me.myklebust.xpdoctor.validator.nodevalidator.uniquepath;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

import me.myklebust.xpdoctor.validator.ValidatorResult;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.branch.Branches;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.script.serializer.MapGenerator;

public class NotUniquePathResult
    implements ValidatorResult
{

    public static final String TYPE_NAME = "NotUniquePath";

    private final NodePath nodePath;

    private final List<NotUniqueEntry> entries;

    private NotUniquePathResult( final Builder builder )
    {
        nodePath = builder.nodePath;
        entries = builder.entries;
    }

    public static Builder create( final NodePath nodePath )
    {
        return new Builder( nodePath );
    }

    @Override
    public String type()
    {
        return TYPE_NAME;
    }

    @Override
    public void serialize( final MapGenerator gen )
    {
        gen.map();
        gen.value( "type", TYPE_NAME );

        gen.value( "path", this.nodePath );
        serialize( gen, this.entries );
        gen.end();
    }

    private void serialize( final MapGenerator gen, final Collection<NotUniqueEntry> entries )
    {
        gen.array( "entries" );
        for ( final NotUniqueEntry entry : entries )
        {
            serialize( gen, entry );
        }
        gen.end();
    }

    private void serialize( final MapGenerator gen, final NotUniqueEntry entry )
    {
        gen.map();
        gen.value( "timestamp", entry.getInstant() );
        gen.value( "nodeId", entry.getNodeId() );
        serialize( gen, entry.getBranches() );
        gen.end();
    }

    private void serialize( final MapGenerator gen, final Branches branches )
    {
        final MapGenerator array = gen.array( "branches" );

        for ( final Branch branch : branches )
        {
            array.value( branch.getValue() );
        }
        gen.end();
    }

    public static final class Builder
    {
        private final NodePath nodePath;

        private List<NotUniqueEntry> entries = Lists.newArrayList();

        private Builder( final NodePath nodePath )
        {
            this.nodePath = nodePath;
        }

        public Builder add( final NotUniqueEntry val )
        {
            entries.add( val );
            return this;
        }

        public NotUniquePathResult build()
        {
            return new NotUniquePathResult( this );
        }
    }
}
