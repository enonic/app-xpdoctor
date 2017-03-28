package me.myklebust.xpdoctor.validator.mapper;

import java.util.Collection;

import com.google.common.collect.Multimap;

import me.myklebust.xpdoctor.validator.ValidationError;
import me.myklebust.xpdoctor.validator.ValidatorEntry;
import me.myklebust.xpdoctor.validator.ValidatorResults;

import com.enonic.xp.script.serializer.MapGenerator;
import com.enonic.xp.script.serializer.MapSerializable;

public class ValidatorResultsMapper
    implements MapSerializable
{
    private final ValidatorResults results;

    public ValidatorResultsMapper( final ValidatorResults results )
    {
        this.results = results;
    }

    @Override
    public void serialize( final MapGenerator gen )
    {
        gen.value( "totalIssues", results.getErrors().size() );

        gen.array( "results" );

        final Multimap<ValidatorEntry, ValidationError> entries = results.getErrors();

        for ( final ValidatorEntry entry : entries.keySet() )
        {
            gen.map();
            gen.value( "path", entry.getNodePath() );
            gen.value( "id", entry.getNodeId() );
            mapErrors( gen, entries, entry );
            gen.end();
        }
        gen.end();
    }

    private void mapErrors( final MapGenerator gen, final Multimap<ValidatorEntry, ValidationError> entries, final ValidatorEntry entry )
    {
        gen.array( "issues" );
        final Collection<ValidationError> errors = entries.get( entry );

        for ( final ValidationError error : errors )
        {
            gen.value( error.name() );
        }
        gen.end();
    }
}
