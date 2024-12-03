package me.myklebust.xpdoctor.validator.nodevalidator.parentexists;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.Validator;
import me.myklebust.xpdoctor.validator.ValidatorResults;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.task.ProgressReporter;

@Component(immediate = true)
public class ParentExistsValidator
    implements Validator
{
    private NodeService nodeService;

    private NoParentDoctor doctor;

    @Activate
    public void activate()
    {
        this.doctor = new NoParentDoctor( this.nodeService );
    }

    @Override
    public int order()
    {
        return 2;
    }

    @Override
    public String name()
    {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getDescription()
    {
        return "Validates that a node has a valid parent";
    }

    @Override
    public String getRepairStrategy()
    {
        return "";
    }

    @Override
    public ValidatorResults validate( final ProgressReporter reporter )
    {
        return ParentExistsExistsExecutor.create().
            nodeService( this.nodeService ).
            progressReporter( reporter ).
            validatorName( name() ).
            build().
            execute();
    }

    @Override
    public RepairResult repair( final NodeId nodeId )
    {
        return this.doctor.repairNode( nodeId, true );
    }

    @Reference
    public void setNodeService( final NodeService nodeService )
    {
        this.nodeService = nodeService;
    }
}
