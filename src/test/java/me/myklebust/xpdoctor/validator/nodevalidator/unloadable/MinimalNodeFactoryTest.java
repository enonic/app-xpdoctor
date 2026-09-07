package me.myklebust.xpdoctor.validator.nodevalidator.unloadable;

import java.time.Instant;

import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;

import com.enonic.xp.blob.BlobKey;
import com.enonic.xp.blob.BlobKeys;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.node.NodeVersion;
import com.enonic.xp.node.NodeVersionId;
import com.enonic.xp.node.NodeVersionKey;

public class MinimalNodeFactoryTest
{
    @Test
    public void name()
        throws Exception
    {
        final ByteSource source = MinimalNodeFactory.create( "minimal_content.json", NodeVersion.create().
            nodeId( NodeId.from( "fisk" ) ).
            nodeVersionId( NodeVersionId.from( "v1" ) ).
            nodeVersionKey( NodeVersionKey.create().
                                nodeBlobKey( BlobKey.from( "a" ) ).
                                indexConfigBlobKey( BlobKey.from( "b" ) ).
                                accessControlBlobKey( BlobKey.from( "c" ) ).
                                build() ).
            binaryBlobKeys( BlobKeys.empty() ).
            timestamp( Instant.now() ).
            nodePath( new NodePath( "/fisk/ost/løk" ) ).
            build() );

        System.out.println( source.asCharSource( Charsets.UTF_8 ).read() );

    }
}