package me.myklebust.xpdoctor.validator.mapper;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import me.myklebust.xpdoctor.json.JsonMapGenerator;
import me.myklebust.xpdoctor.validator.BranchValidationResult;
import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.RepoValidationResult;
import me.myklebust.xpdoctor.validator.RepoValidationResults;
import me.myklebust.xpdoctor.validator.ValidatorResult;
import me.myklebust.xpdoctor.validator.ValidatorResults;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.node.NodeVersionId;
import com.enonic.xp.repository.RepositoryId;

public class RepoResultsMapperTest
{
    private JsonMapGenerator generator;

    private ObjectMapper mapper;

    @Before
    public void setUp()
        throws Exception
    {
        this.generator = new JsonMapGenerator();

        this.mapper = new ObjectMapper();
        this.mapper.enable( SerializationFeature.INDENT_OUTPUT );
        this.mapper.enable( SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS );
        this.mapper.enable( SerializationFeature.WRITE_NULL_MAP_VALUES );
    }

    @Test
    public void name()
        throws Exception
    {
        final RepoValidationResults result = createResult();

        final RepoResultsMapper mapper = new RepoResultsMapper( result );

        mapper.serialize( generator );

        final String resultString = this.mapper.writeValueAsString( generator.getRoot() );
        System.out.println( resultString );
    }

    private RepoValidationResults createResult()
    {
        return RepoValidationResults.create().
            add( RepoValidationResult.create( RepositoryId.from( "my-repo" ) ).
                add( createBranch( "draft" ) ).
                add( createBranch( "master" ) ).
                build() ).
            add( RepoValidationResult.create( RepositoryId.from( "other-repo" ) ).
                add( createBranch( "master" ) ).
                build() ).
            build();
    }

    private BranchValidationResult createBranch( final String name )
    {
        return BranchValidationResult.create( Branch.from( name ) ).
            results( ValidatorResults.create().
                add( createValidatorResult() ).
                add( createValidatorResult() ).
                build() ).
            build();
    }

    private ValidatorResult createValidatorResult()
    {
        return ValidatorResult.create().
            type( "Missing parent" ).
            message( "myMessage" ).
            nodeId( NodeId.from( "abc" ) ).
            nodePath( NodePath.ROOT ).
            nodeVersionId( NodeVersionId.from( "123" ) ).
            repairResult( RepairResult.create().
                message( "not fixed" ).
                repairStatus( RepairStatus.NOT_REPAIRABLE ).
                build() ).
            build();
    }
}
