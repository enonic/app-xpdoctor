package me.myklebust.xpdoctor.validator.nodevalidator.unloadable;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import me.myklebust.xpdoctor.validator.Validator;
import me.myklebust.xpdoctor.validator.ValidatorResults;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.task.ProgressReporter;

@Component(immediate = true)
public class LoadableNodeValidator
    implements Validator
{
    private NodeService nodeService;

    @Override
    public String name()
    {
        return "LoadableNodeValidator";
    }

    @Override
    public String getDescription()
    {
        return "Validates that a node is loadable. If a node is not loadable, its usually caused by a missing file in repo/node";
    }

    @Override
    public String getRepairStrategy()
    {
        return "To repaid a node with a missing file, I try rolling back to previous version to see if any of these works.";
    }

    @Override
    public ValidatorResults validate( final ProgressReporter reporter )
    {
        return new LoadableNodeExecutor( this.nodeService, reporter ).execute();
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
