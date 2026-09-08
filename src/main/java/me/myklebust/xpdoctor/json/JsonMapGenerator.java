package me.myklebust.xpdoctor.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.enonic.xp.script.serializer.MapGeneratorBase;

public final class JsonMapGenerator
    extends MapGeneratorBase
{
    public JsonMapGenerator()
    {
        initRoot();
    }

    @Override
    protected Object newMap()
    {
        return new LinkedHashMap<String, Object>();
    }

    @Override
    protected Object newArray()
    {
        return new ArrayList<>();
    }

    @Override
    protected Object newFunction( final Function<?, ?> function )
    {
        return function;
    }

    @Override
    protected boolean isMap( final Object value )
    {
        return value instanceof Map;
    }

    @Override
    protected boolean isArray( final Object value )
    {
        return value instanceof List;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void putInMap( final Object map, final String key, final Object value )
    {
        ( (Map<String, Object>) map ).put( key, value );
    }

    @Override
    protected void putRawValueInMap( final Object map, final String key, final Object value )
    {
        putInMap( map, key, value );
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void addToArray( final Object array, final Object value )
    {
        ( (List<Object>) array ).add( value );
    }

    @Override
    protected MapGeneratorBase newGenerator()
    {
        return new JsonMapGenerator();
    }
}
