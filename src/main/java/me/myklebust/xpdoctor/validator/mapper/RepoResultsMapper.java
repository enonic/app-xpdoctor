package me.myklebust.xpdoctor.validator.mapper;

import me.myklebust.xpdoctor.validator.result.BranchValidationResult;
import me.myklebust.xpdoctor.validator.result.RepoValidationResult;
import me.myklebust.xpdoctor.validator.result.RepoValidationResults;

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
        gen.value( "timestamp", results.getTimestamp() );
        gen.value( "totalIssues", results.getTotalIssues() );
        gen.array( "repositories" );
        results.getRepositories().forEach( repo -> serialize( gen, repo ) );
        gen.end();
    }

    private void serialize( final MapGenerator gen, final RepoValidationResult repo )
    {
        gen.map();
        gen.value( "id", repo.getRepositoryId().toString() );
        gen.array( "branches" );
        repo.getBranches().forEach( branch -> serialize( gen, branch ) );
        gen.end();
        gen.end();
    }

    private void serialize( final MapGenerator gen, final BranchValidationResult result )
    {
        gen.map();
        gen.value( "branch", result.getBranch().toString() );
        new ValidatorResultsMapper( result.getResults() ).serialize( gen );
        gen.end();
    }


}
