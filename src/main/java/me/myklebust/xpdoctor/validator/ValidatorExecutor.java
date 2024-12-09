package me.myklebust.xpdoctor.validator;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.branch.Branches;
import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.repository.Repositories;
import com.enonic.xp.repository.Repository;
import com.enonic.xp.repository.RepositoryId;
import com.enonic.xp.repository.RepositoryService;
import com.enonic.xp.security.IdProviderKey;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.User;
import com.enonic.xp.security.auth.AuthenticationInfo;
import com.enonic.xp.task.ProgressReporter;

public class ValidatorExecutor
{
    private static final Logger LOG = LoggerFactory.getLogger( ValidatorExecutor.class );

    private final RepositoryService repoService;

    private final List<Validator> validators;

    private final ProgressReporter progressReporter;

    private final Map<RepositoryId, Branches> repoBranches;


    private ValidatorExecutor( final Builder builder )
    {
        repoService = builder.repoService;
        validators = builder.validators;
        progressReporter = builder.progressReporter;
        repoBranches = builder.repoBranches;

    }

    public RepoValidationResults execute()
    {
        final RepoValidationResults.Builder results = RepoValidationResults.create();
        final Repositories repositories = this.repoService.list();

        repositories.stream().
            filter( repository -> repoBranches.containsKey( repository.getId() ) ).
            forEach( ( repo ) -> {
                final RepositoryId repositoryId = repo.getId();

                LOG.info( "Checking repo: [{}]", repositoryId );

                final RepoValidationResult.Builder repoBuilder = RepoValidationResult.create( repositoryId );

                final Branches branches = repo.getBranches();

            branches.stream().filter( branch -> repoBranches.get( repositoryId ).contains( branch ) ).forEach( branch -> {
                LOG.info( "Checking branch: [{}]", branch );
                final ValidatorResults validationResults = createContext( repositoryId, branch ).callWith( this::doExecute );
                final BranchValidationResult.Builder branchResult = BranchValidationResult.create( branch ).results( validationResults );
                repoBuilder.add( branchResult.build() );
            } );

                results.add( repoBuilder.build() );
            } );

        progressReporter.info( "Done, all validators finished" );
        progressReporter.progress( this.validators.size(), this.validators.size() );

        return results.build();
    }


    private ValidatorResults doExecute()
    {
        final ValidatorResults.Builder result = ValidatorResults.create();

        runNodeValidators( result );

        return result.build();
    }

    private void runNodeValidators( final ValidatorResults.Builder results )
    {
        this.validators.forEach( validator -> {
            final ValidatorResults validate = validator.validate( this.progressReporter );
            results.add( validate );
        } );
    }


    private Context createContext( final RepositoryId repositoryId, final Branch branch )
    {
        final PrincipalKey superUser = PrincipalKey.ofUser( IdProviderKey.system(), "su" );

        final User admin = User.create().key( superUser ).login( "su" ).build();
        final AuthenticationInfo authInfo = AuthenticationInfo.create().principals( RoleKeys.ADMIN ).user( admin ).build();

        return ContextBuilder.from( ContextAccessor.current() ).
            authInfo( authInfo ).
            repositoryId( repositoryId ).
            branch( branch ).
            build();
    }

    public static Builder create()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private RepositoryService repoService;

        private List<Validator> validators;

        private ProgressReporter progressReporter;

        private Map<RepositoryId, Branches> repoBranches;

        private Builder()
        {
        }

        public Builder repoService( final RepositoryService val )
        {
            repoService = val;
            return this;
        }

        public Builder repoBranches( final Map<RepositoryId, Branches> val )
        {
            repoBranches = val;
            return this;
        }


        public Builder validators( final List<Validator> val )
        {
            validators = val;
            return this;
        }


        public Builder progressReporter( final ProgressReporter val )
        {
            progressReporter = val;
            return this;
        }

        public ValidatorExecutor build()
        {
            return new ValidatorExecutor( this );
        }
    }
}
