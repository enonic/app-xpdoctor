package me.myklebust.xpdoctor.validator;

import java.util.Set;

import com.google.common.collect.Sets;

import me.myklebust.xpdoctor.validator.mapper.RepoResultsMapper;
import me.myklebust.xpdoctor.validator.nodevalidator.NodeValidator;
import me.myklebust.xpdoctor.validator.nodevalidator.parentexists.ParentExistsNodeValidator;
import me.myklebust.xpdoctor.validator.nodevalidator.uniquepath.UniquePathNodeValidator;
import me.myklebust.xpdoctor.validator.nodevalidator.unloadable.LoadableNodeValidator;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.branch.Branches;
import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.repository.Repositories;
import com.enonic.xp.repository.RepositoryId;
import com.enonic.xp.repository.RepositoryService;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.User;
import com.enonic.xp.security.UserStoreKey;
import com.enonic.xp.security.auth.AuthenticationInfo;

@SuppressWarnings("unused")
public class IntegrityBean
    implements ScriptBean
{
    private NodeService nodeService;

    private RepositoryService repoService;

    private final Set<NodeValidator> nodeValidators = Sets.newHashSet();

    @SuppressWarnings("unused")
    public Object execute()
    {
        final RepoValidationResults.Builder results = RepoValidationResults.create();

        final Repositories repositories = this.repoService.list();
        repositories.forEach( ( repo ) -> {

            final RepoValidationResult.Builder repoBuilder = RepoValidationResult.create( repo.getId() );

            final Branches branches = repo.getBranches();
            branches.stream().forEach( branch -> {
                final ValidatorResults validationResults = createContext( repo.getId(), branch ).callWith( this::doExecute );
                final BranchValidationResult.Builder branchResult = BranchValidationResult.create( branch ).results( validationResults );
                repoBuilder.add( branchResult.build() );
            } );

            results.add( repoBuilder.build() );
        } );

        return new RepoResultsMapper( results.build() );
    }

    private ValidatorResults doExecute()
    {
        final ValidatorResults.Builder result = ValidatorResults.create();

        runNodeValidators( result );

        return result.build();
    }

    private void runNodeValidators( final ValidatorResults.Builder results )
    {
        this.nodeValidators.forEach( validator -> {
            final ValidatorResults validate = validator.validate();
            results.add( validate );
        } );
    }

    private Context createContext( final RepositoryId repositoryId, final Branch branch )
    {
        final PrincipalKey superUser = PrincipalKey.ofUser( UserStoreKey.system(), "su" );

        final User admin = User.create().key( superUser ).login( "su" ).build();
        final AuthenticationInfo authInfo = AuthenticationInfo.create().principals( RoleKeys.ADMIN ).user( admin ).build();

        return ContextBuilder.from( ContextAccessor.current() ).
            authInfo( authInfo ).
            repositoryId( repositoryId ).
            branch( branch ).
            build();
    }

    @Override
    public void initialize( final BeanContext context )
    {
        this.nodeService = context.getService( NodeService.class ).get();
        this.repoService = context.getService( RepositoryService.class ).get();
        nodeValidators.add( new ParentExistsNodeValidator( this.nodeService ) );
        nodeValidators.add( new UniquePathNodeValidator( this.nodeService, this.repoService ) );
        nodeValidators.add( new LoadableNodeValidator( this.nodeService ) );
    }
}
