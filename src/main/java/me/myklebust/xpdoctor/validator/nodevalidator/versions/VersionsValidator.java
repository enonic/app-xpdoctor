package me.myklebust.xpdoctor.validator.nodevalidator.versions;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.Validator;
import me.myklebust.xpdoctor.validator.ValidatorResults;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.task.ProgressReporter;

@Component(immediate = true)
public class VersionsValidator
    implements Validator
{
    private NodeService nodeService;

    @Activate
    public void activate()
    {
    }

    @Override
    public int order()
    {
        return 4;
    }

    @Override
    public String name()
    {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getDescription()
    {
        return "Validates that node versions are loadable";
    }

    @Override
    public String getRepairStrategy()
    {
        return "";
    }

    @Override
    public ValidatorResults validate( final ProgressReporter reporter )
    {
        return VersionsExecutor.create().
            nodeService( this.nodeService ).
            progressReporter( reporter ).
            validatorName( name() ).
            build().
            execute();
    }

    @Override
    public RepairResult repair( final NodeId nodeId )
    {
        return RepairResult.create().repairStatus( RepairStatus.NOT_REPAIRABLE ).message( "cannot automatically fix broken versions" ).build();
    }

    @Reference
    public void setNodeService( final NodeService nodeService )
    {
        this.nodeService = nodeService;
    }
}
