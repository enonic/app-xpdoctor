package me.myklebust.xpdoctor.validator.nodevalidator.unloadable;

import java.util.HashMap;
import java.util.Map;

import com.enonic.xp.node.NodeVersion;

class Substitutor
{
    static String substitute( final String source, final NodeVersion version )
    {
        final Map<String, String> valueMap = new HashMap<>();
        valueMap.put( "name", version.getNodePath().getName() + " [xpDoctor-revived]" );
        valueMap.put( "nodeId", version.getNodeId().toString() );
        valueMap.put( "timestamp", version.getTimestamp().toString() );

        String result = source;
        for ( final Map.Entry<String, String> entry : valueMap.entrySet() )
        {
            result = result.replace( "${" + entry.getKey() + "}", entry.getValue() );
        }

        return result;
    }

}
