package me.myklebust.xpdoctor.validator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.branch.Branches;
import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.node.NodeQuery;
import com.enonic.xp.node.NodeService;
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

    private final NodeService nodeService;

    private final List<Validator> validators;

    private final ProgressReporter progressReporter;

    private final Map<RepositoryId, Branches> repoBranches;

    private final BooleanSupplier cancelled;


    private ValidatorExecutor( final Builder builder )
    {
        repoService = builder.repoService;
        nodeService = builder.nodeService;
        validators = builder.validators;
        progressReporter = builder.progressReporter;
        repoBranches = builder.repoBranches;
        cancelled = builder.cancelled;
    }

    public RepoValidationResults execute()
    {
        final RepoValidationResults.Builder results = RepoValidationResults.create();
        final List<Repository> repositories =
            this.repoService.list().stream().filter( repository -> repoBranches.containsKey( repository.getId() ) ).toList();

        final Map<RepositoryId, Map<Branch, Integer>> nodeCounts = countNodes( repositories );
        final int grandTotal =
            nodeCounts.values().stream().flatMap( branches -> branches.values().stream() ).mapToInt( count -> count * validators.size() ).sum();
        final AnalysisProgressReporter progress = new AnalysisProgressReporter( this.progressReporter, grandTotal );

        repositories.forEach( ( repo ) -> {
            final RepositoryId repositoryId = repo.getId();

            LOG.info( "Checking repo: [{}]", repositoryId );

            final RepoValidationResult.Builder repoBuilder = RepoValidationResult.create( repositoryId );

            selectedBranches( repo ).forEach( branch -> {
                LOG.info( "Checking branch: [{}]", branch );
                final int nodeCount = nodeCounts.get( repositoryId ).get( branch );
                final ValidatorResults validationResults =
                    createContext( repositoryId, branch ).callWith( () -> doExecute( progress, repositoryId, branch, nodeCount ) );
                final BranchValidationResult.Builder branchResult =
                    BranchValidationResult.create( branch ).nodeCount( nodeCount ).results( validationResults );
                repoBuilder.add( branchResult.build() );
            } );

            results.add( repoBuilder.build() );
        } );

        progress.info( "Done, all validators finished" );
        progress.done();

        return results.build();
    }

    private Map<RepositoryId, Map<Branch, Integer>> countNodes( final List<Repository> repositories )
    {
        final Map<RepositoryId, Map<Branch, Integer>> nodeCounts = new HashMap<>();
        repositories.forEach( repo -> {
            final Map<Branch, Integer> branchCounts = new HashMap<>();
            selectedBranches( repo ).forEach( branch -> branchCounts.put( branch, createContext( repo.getId(), branch ).callWith(
                () -> (int) nodeService.findByQuery( NodeQuery.create().size( 0 ).build() ).getTotalHits() ) ) );
            nodeCounts.put( repo.getId(), branchCounts );
        } );
        return nodeCounts;
    }

    private List<Branch> selectedBranches( final Repository repo )
    {
        return repo.getBranches().stream().filter( branch -> repoBranches.get( repo.getId() ).contains( branch ) ).toList();
    }

    private ValidatorResults doExecute( final AnalysisProgressReporter progress, final RepositoryId repositoryId, final Branch branch,
                                        final int nodeCount )
    {
        final ValidatorResults.Builder result = ValidatorResults.create();

        runNodeValidators( result, progress, repositoryId, branch, nodeCount );

        return result.build();
    }

    private void runNodeValidators( final ValidatorResults.Builder results, final AnalysisProgressReporter progress,
                                    final RepositoryId repositoryId, final Branch branch, final int nodeCount )
    {
        this.validators.forEach( validator -> {
            if ( cancelled.getAsBoolean() )
            {
                throw new AnalysisCancelledException();
            }
            progress.startValidator( repositoryId, branch, validator.name(), nodeCount );
            final ValidatorResults validate = validator.validate( progress );
            progress.validatorDone();
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

        private NodeService nodeService;

        private List<Validator> validators;

        private ProgressReporter progressReporter;

        private Map<RepositoryId, Branches> repoBranches;

        private BooleanSupplier cancelled = () -> false;

        private Builder()
        {
        }

        public Builder repoService( final RepositoryService val )
        {
            repoService = val;
            return this;
        }

        public Builder nodeService( final NodeService val )
        {
            nodeService = val;
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

        public Builder cancelled( final BooleanSupplier val )
        {
            cancelled = val;
            return this;
        }

        public ValidatorExecutor build()
        {
            return new ValidatorExecutor( this );
        }
    }
}
