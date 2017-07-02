package me.myklebust.xpdoctor.validator;

import com.enonic.xp.script.serializer.MapGenerator;

public interface ValidatorResult
{
    void serialize( MapGenerator gen );

    String type();
}
