package me.myklebust.xpdoctor.validator.nodevalidator.uniquepath;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.Validator;
import me.myklebust.xpdoctor.validator.ValidatorResults;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.repository.RepositoryService;
import com.enonic.xp.task.ProgressReporter;

@Component(immediate = true)
public class UniquePathValidator
    implements Validator
{
    private RepositoryService repositoryService;

    private NodeService nodeService;

    private UniquePathDoctor doctor;

    @Activate
    public void activate()
    {
        this.doctor = new UniquePathDoctor( this.nodeService );
    }

    @Override
    public int order()
    {
        return 1;
    }

    @Override
    public String name()
    {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getDescription()
    {
        return "Validates that a node has a unique path";
    }

    @Override
    public String getRepairStrategy()
    {
        return "";
    }

    @Override
    public ValidatorResults validate( final ProgressReporter reporter )
    {
        return UniquePathValidatorExecutor.create().
            nodeService( this.nodeService ).
            repositoryService( this.repositoryService ).
            progressReporter( reporter ).
            validatorName( this.name() ).
            build().
            execute();
    }

    @Override
    public RepairResult repair( final NodeId nodeId )
    {
        return this.doctor.repairNode( nodeId, true );
    }

    @Reference
    public void setRepositoryService( final RepositoryService repositoryService )
    {
        this.repositoryService = repositoryService;
    }

    @Reference
    public void setNodeService( final NodeService nodeService )
    {
        this.nodeService = nodeService;
    }

    @Override
    public int compareTo( final Validator o )
    {
        return Integer.compare( this.order(), o.order() );
    }
}
