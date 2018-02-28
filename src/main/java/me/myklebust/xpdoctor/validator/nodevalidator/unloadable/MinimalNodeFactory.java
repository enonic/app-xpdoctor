package me.myklebust.xpdoctor.validator.nodevalidator.unloadable;

import java.io.IOException;
import java.net.URL;

import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;

import com.enonic.xp.node.NodeVersionMetadata;

import static com.google.common.io.Resources.getResource;

class MinimalNodeFactory
{
    static ByteSource create( final String name, final NodeVersionMetadata version )
    {
        final String resourceValue = loadResource( name );

        return ByteSource.wrap( Substitutor.substitute( resourceValue, version ).getBytes() );
    }

    private static String loadResource( String resource )
    {
        URL url = getResource( MinimalNodeFactory.class, resource );
        try
        {
            return com.google.common.io.Resources.toString( url, Charsets.UTF_8 );
        }
        catch ( IOException ex )
        {
            throw new RuntimeException( ex );
        }
    }
}
