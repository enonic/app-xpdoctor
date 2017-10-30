package me.myklebust.xpdoctor.validator.mapper;

import me.myklebust.xpdoctor.validator.RepairResult;

import com.enonic.xp.script.serializer.MapGenerator;
import com.enonic.xp.script.serializer.MapSerializable;

public class RepairResultMapper
    implements MapSerializable
{
    private final RepairResult repairResult;

    public RepairResultMapper( final RepairResult repairResult )
    {
        this.repairResult = repairResult;
    }

    @Override
    public void serialize( final MapGenerator gen )
    {
        gen.value( "message", repairResult.message() );
        gen.value( "status", repairResult.status() );
    }
}
