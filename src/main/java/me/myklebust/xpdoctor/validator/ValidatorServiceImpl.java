package me.myklebust.xpdoctor.validator;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.repository.RepositoryService;

@SuppressWarnings("unused")
@Component(immediate = true)
public class ValidatorServiceImpl
    implements ValidatorService
{
    private final Set<Validator> validators = new TreeSet<>( Comparator.comparingInt( Validator::order ) );

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

        this.repoService.invalidateAll();

        return ValidatorExecutor.create().
            repoService( this.repoService ).
            progressReporter( params.getProgressReporter() ).
            repositoryId( params.getRepositoryId() ).
            branch( params.getBranch() ).
            validators( this.validators.stream().filter( validator -> params.getEnabledValidators().contains( validator.name() ) ).collect(
                Collectors.toList() ) ).
            build().
            execute();
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
    public void removeValidator( final Validator val )
    {
        this.validators.remove( val );
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



