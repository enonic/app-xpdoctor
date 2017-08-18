package me.myklebust.xpdoctor.validator.mapper;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import me.myklebust.xpdoctor.validator.BranchValidationResult;
import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepoValidationResult;
import me.myklebust.xpdoctor.validator.RepoValidationResults;
import me.myklebust.xpdoctor.validator.ValidatorResult;
import me.myklebust.xpdoctor.validator.ValidatorResults;

import com.enonic.xp.script.serializer.MapGenerator;
import com.enonic.xp.script.serializer.MapSerializable;

public class RepoResultsMapper
    implements MapSerializable
{
    private final RepoValidationResults results;

    public RepoResultsMapper( final RepoValidationResults results )
    {
        this.results = results;
    }

    @Override
    public void serialize( final MapGenerator gen )
    {
        gen.map( "results" );
        results.getRepositories().forEach( repo -> serialize( gen, repo ) );
        gen.end();
    }

    private void serialize( final MapGenerator gen, final RepoValidationResult result )
    {
        gen.map( result.getRepositoryId().toString() );
        result.getBranches().forEach( branch -> serialize( gen, branch ) );
        gen.end();
    }

    private void serialize( final MapGenerator gen, final BranchValidationResult result )
    {
        gen.map( result.getBranch().toString() );
        serialize( gen, result.getResults() );
        gen.end();
    }

    private void serialize( final MapGenerator gen, final ValidatorResults results )
    {
        final List<ValidatorResult> allResults = results.getResults();
        gen.value( "totalIssues", allResults.size() );
        serializeTypes( gen, allResults );

        gen.array( "results" );
        for ( final ValidatorResult entry : allResults )
        {
            serialize( gen, entry );
        }

        gen.end();
    }

    private void serializeTypes( final MapGenerator gen, final List<ValidatorResult> allResults )
    {
        final Map<String, Integer> types = Maps.newHashMap();

        for ( final ValidatorResult entry : allResults )
        {
            addTypeEntry( types, entry );
        }

        serialize( gen, types );
    }

    private void serialize( final MapGenerator gen, final ValidatorResult result )
    {
        gen.map();
        gen.value( "type", result.type() );
        gen.value( "id", result.nodeId() );
        gen.value( "message", result.message() );
        gen.value( "path", result.nodePath() );
        serialize( gen, result.repairResult() );
        gen.value( "timestamp", result.timestamp() );
        gen.end();
    }

    private void serialize( final MapGenerator gen, final RepairResult repairResult )
    {
        gen.map( "repair" );
        gen.value( "message", repairResult.message() );
        gen.value( "status", repairResult.status() );
        gen.end();
    }


    private void serialize( final MapGenerator gen, final Map<String, Integer> types )
    {
        gen.map( "types" );
        types.keySet().forEach( key -> gen.value( key, types.get( key ) ) );
        gen.end();
    }

    private void addTypeEntry( final Map<String, Integer> types, final ValidatorResult entry )
    {
        final String type = entry.type();
        int count = types.containsKey( type ) ? types.get( type ) : 0;
        types.put( type, count + 1 );
    }
}
