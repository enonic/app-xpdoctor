package me.myklebust.xpdoctor.validator;

import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import com.enonic.xp.node.NodeService;
import com.enonic.xp.repository.RepositoryService;

@SuppressWarnings("unused")
@Component(immediate = true)
public class ValidatorServiceImpl
    implements ValidatorService
{
    private Set<Validator> validators = Sets.newHashSet();

    private NodeService nodeService;

    private RepositoryService repoService;

    private final Logger LOG = LoggerFactory.getLogger( IntegrityBean.class );

    public Set<Validator> getValidators()
    {
        return validators;
    }

    public RepoValidationResults execute( final ValidatorParams params )
    {
        return ValidatorExecutor.create().
          //  nodeService( this.nodeService ).
            repoService( this.repoService ).
            progressReporter( params.getProgressReporter() ).
            validators( this.validators ).
            build().
            execute();
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



