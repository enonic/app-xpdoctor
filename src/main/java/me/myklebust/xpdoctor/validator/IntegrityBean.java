package me.myklebust.xpdoctor.validator;

import java.util.Set;

import com.google.common.collect.Sets;

import me.myklebust.xpdoctor.validator.mapper.ValidatorResultsMapper;
import me.myklebust.xpdoctor.validator.nodevalidator.NodeValidator;
import me.myklebust.xpdoctor.validator.nodevalidator.ParentExistsNodeValidator;
import me.myklebust.xpdoctor.validator.nodevalidator.UniquePathNodeValidator;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.node.FindNodesByQueryResult;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeNotFoundException;
import com.enonic.xp.node.NodeQuery;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.repository.RepositoryId;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.User;
import com.enonic.xp.security.UserStoreKey;
import com.enonic.xp.security.auth.AuthenticationInfo;

public class IntegrityBean
    implements ScriptBean
{
    private NodeService nodeService;

    private final int batchSize = 1000;

    private final Set<NodeValidator> nodeValidators = Sets.newHashSet();

    @SuppressWarnings("unused")
    public Object execute( final String repositoryId, final String branch )
    {
        System.out.println( "Executing stuff" );

        final Context context = createContext( RepositoryId.from( repositoryId ), Branch.from( branch ) );

        final ValidatorResults results = context.callWith( this::doExecute );

        return new ValidatorResultsMapper( results );
    }

    private ValidatorResults doExecute()
    {
        return executeNodeValidators();
    }

    private ValidatorResults executeNodeValidators()
    {
        final long totalEntries = getTotalEntries();

        final ValidatorResults results = new ValidatorResults();

        int processed = 0;

        while ( processed <= totalEntries )
        {
            processed = executeBatch( processed, results );
        }
        return results;
    }

    private int executeBatch( int processed, final ValidatorResults results )
    {
        System.out.println( "Processing [" + processed + "] to [" + ( processed + batchSize ) + "]" );

        final FindNodesByQueryResult result = getBatch( processed );

        result.getNodeIds().forEach( ( nodeId ) -> {

            try
            {
                final Node node = this.nodeService.getById( nodeId );

                runNodeValidators( results, node );
            }
            catch ( final NodeNotFoundException e )
            {
                // results.add( new ValidatorEntry( null, nodeId ), ValidationError.NOT_IN_STORAGE );
            }

        } );

        processed += batchSize;
        return processed;
    }

    private void runNodeValidators( final ValidatorResults results, final Node node )
    {
        this.nodeValidators.forEach( validator -> {
            final ValidatorResult validatorResult = validator.validate( node );
            if ( validatorResult != null )
            {
                results.add( ValidatorEntry.create( validatorResult.getPath(), validatorResult.getNodeId() ), validatorResult.getError() );
            }
        } );
    }

    private FindNodesByQueryResult getBatch( final int processed )
    {
        return nodeService.findByQuery( NodeQuery.create().
            size( batchSize ).
            from( processed ).
            build() );
    }

    private long getTotalEntries()
    {
        final NodeQuery nodeQuery = NodeQuery.create().
            size( 0 ).
            build();

        final FindNodesByQueryResult result = nodeService.findByQuery( nodeQuery );

        System.out.println( "Found : " + result.getTotalHits() + " entries to process" );

        return result.getTotalHits();
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
        nodeValidators.add( new ParentExistsNodeValidator( this.nodeService ) );
        nodeValidators.add( new UniquePathNodeValidator( this.nodeService ) );
    }
}
