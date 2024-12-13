package me.myklebust.xpdoctor.validator.mapper;

import java.util.List;
import java.util.Set;

import me.myklebust.xpdoctor.validator.Validator;

import com.enonic.xp.script.serializer.MapGenerator;
import com.enonic.xp.script.serializer.MapSerializable;

public class ValidatorsMapper
    implements MapSerializable
{
    private final List<Validator> validators;

    public ValidatorsMapper( final List<Validator> validators )
    {
        this.validators = validators;
    }

    @Override
    public void serialize( final MapGenerator gen )
    {
        gen.array( "validators" );
        validators.forEach( validator -> serialize( gen, validator ) );
        gen.end();
    }

    private void serialize( final MapGenerator gen, final Validator validator )
    {
        gen.map();
        gen.value( "name", validator.name() );
        gen.value( "description", validator.getDescription() );
        gen.value( "repairStrategy", validator.getRepairStrategy() );
        gen.end();
    }
}

