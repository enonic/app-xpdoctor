package me.myklebust.xpdoctor.validator.nodevalidator.unloadable;

import java.time.Instant;

import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.node.NodeVersionMetadata;

public class MinimalNodeFactoryTest
{
    @Test
    public void name()
        throws Exception
    {
        final ByteSource source = MinimalNodeFactory.create( "minimal_content.json", NodeVersionMetadata.create().
            nodeId( NodeId.from( "fisk" ) ).
            timestamp( Instant.now() ).
            nodePath( NodePath.create( "/fisk/ost/løk" ).build() ).
            build() );

        System.out.println( source.asCharSource( Charsets.UTF_8 ).read() );

    }
}