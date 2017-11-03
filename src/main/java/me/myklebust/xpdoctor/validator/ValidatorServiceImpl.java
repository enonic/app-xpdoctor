package me.myklebust.xpdoctor.validator;

import java.util.Set;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import me.myklebust.xpdoctor.validator.model.IssueEntries;
import me.myklebust.xpdoctor.validator.result.RepoValidationResults;
import me.myklebust.xpdoctor.validator.result.ValidatorResult;
import me.myklebust.xpdoctor.validator.result.ValidatorResults;

import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.repository.RepositoryService;

@SuppressWarnings("unused")
@Component(immediate = true)
public class ValidatorServiceImpl
    implements ValidatorService
{
    private Set<Validator> validators = Sets.newTreeSet();

    private NodeService nodeService;

    private RepositoryService repoService;

    private final Logger LOG = LoggerFactory.getLogger( IntegrityBean.class );

    public Set<Validator> getValidators()
    {
        return validators;
    }

    public Validator getValidator( final String validatorName )
    {
        for ( final Validator validator : this.validators )
        {
            if ( validator.name().equals( validatorName ) )
            {
                return validator;
            }
        }

        return null;
    }

    public RepoValidationResults analyze( final AnalyzeParams params )
    {
        return MultiRepoValidatorExecutor.create().
            repoService( this.repoService ).
            progressReporter( params.getProgressReporter() ).
            validators( this.validators.stream().filter( validator -> params.getEnabledValidators().contains( validator.name() ) ).collect(
                Collectors.toList() ) ).
            build().
            execute();
    }

    public ValidatorResults reAnalyze( final IssueEntries issues )
    {
        final ValidatorResults.Builder builder = ValidatorResults.create();

        issues.forEach( issue -> {

            ContextBuilder.from( ContextAccessor.current() ).
                repositoryId( issue.getRepoId() ).
                branch( issue.getBranch() ).
                build().
                callWith( () -> builder.add( validateIssue( issue.getNodeId() ) ) );
        } );

        return builder.build();
    }

    private ValidatorResults validateIssue( final NodeId nodeId )
    {
        final ValidatorResults.Builder builder = ValidatorResults.create();

        this.validators.forEach( validator -> {
            final ValidatorResult result = validator.validate( nodeId );
            if ( result != null )
            {
                builder.add( result );
            }
        } );

        return builder.build();
    }

    public void repair( final NodeId nodeId )
    {
        validators.forEach( validator -> {
            validator.repair( nodeId );
        } );
    }

    @SuppressWarnings("unused")
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addValidator( final Validator val )
    {
        LOG.info( "Adding validator: " + val.name() );
        this.validators.add( val );
    }

    @SuppressWarnings("unused")
    @Reference
    public void setNodeService( final NodeService nodeService )
    {
        this.nodeService = nodeService;
    }

    @SuppressWarnings("unused")
    @Reference
    public void setRepoService( final RepositoryService repoService )
    {
        this.repoService = repoService;
    }
}



