package me.myklebust.xpdoctor.validator.nodevalidator.parentexists;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import me.myklebust.xpdoctor.validator.Validator;
import me.myklebust.xpdoctor.validator.ValidatorResults;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.task.ProgressReporter;

@Component(immediate = true)
public class ParentExistsNodeValidator
    implements Validator
{
    private NodeService nodeService;


    @Override
    public String name()
    {
        return "Parent exists validator";
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
        return new ParentExistsExistsExecutor( this.nodeService, reporter ).execute();
    }

    @Override
    public boolean repair( final NodeId nodeId )
    {
        return false;
    }

    @Reference
    public void setNodeService( final NodeService nodeService )
    {
        this.nodeService = nodeService;
    }
}
