package me.myklebust.xpdoctor.validator;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.xp.node.NodeService;
import com.enonic.xp.repository.RepositoryService;

@SuppressWarnings("unused")
@Component(immediate = true)
public class ValidatorServiceImpl
    implements ValidatorService
{
    private static final Logger LOG = LoggerFactory.getLogger( ValidatorServiceImpl.class );

    private final Set<Validator> validators = new ConcurrentSkipListSet<>( Comparator.comparing( Validator::name ) );

    @Reference
    private NodeService nodeService;

    @Reference
    private RepositoryService repoService;

    public List<Validator> getValidators()
    {
        return validators.stream().sorted( Comparator.comparingInt( Validator::order ) ).collect( Collectors.toUnmodifiableList() );
    }

    public Validator getValidator( final String validatorName )
    {
        return validators.stream().filter( validator -> validator.name().equals( validatorName ) ).findAny().orElse( null );
    }

    public RepoValidationResults analyze( final AnalyzeParams params )
    {
        this.repoService.invalidateAll();

        return ValidatorExecutor.create()
            .repoService( this.repoService )
            .progressReporter( params.getProgressReporter() )
            .repoBranches( params.getRepoBranches() )
            .validators( this.validators.stream().filter( validator -> params.getEnabledValidators().contains( validator.name() ) )
                             .sorted( Comparator.comparingInt( Validator::order ) )
                             .collect( Collectors.toUnmodifiableList() ) )
            .build()
            .execute();
    }

    @SuppressWarnings("unused")
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addValidator( final Validator val )
    {
        this.validators.add( val );
    }

    @SuppressWarnings("unused")
    public void removeValidator( final Validator val )
    {
        this.validators.remove( val );
    }
}



