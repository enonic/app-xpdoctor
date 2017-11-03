package me.myklebust.xpdoctor.validator.nodevalidator.parentexists;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.Validator;
import me.myklebust.xpdoctor.validator.result.RepairResult;
import me.myklebust.xpdoctor.validator.result.RepairResultImpl;
import me.myklebust.xpdoctor.validator.result.ValidatorResult;
import me.myklebust.xpdoctor.validator.result.ValidatorResults;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.task.ProgressReporter;

@Component(immediate = true)
public class ParentExistsValidator
    implements Validator
{
    private NodeService nodeService;

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
        return ParentExistsEntriesExecutor.create().
            nodeService( this.nodeService ).
            progressReporter( reporter ).
            validatorName( name() ).
            build().
            execute();
    }

    @Override
    public ValidatorResult validate( final NodeId nodeId )
    {
        return ParentExistsEntryExecutor.create().
            nodeService( this.nodeService ).
            validatorName( this.name() ).
            build().
            execute( nodeId );
    }

    @Override
    public RepairResult repair( final NodeId nodeId )
    {
        return RepairResultImpl.create().
            repairStatus( RepairStatus.UNKNOW ).
            message( "Not implemented yet" ).
            build();
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
