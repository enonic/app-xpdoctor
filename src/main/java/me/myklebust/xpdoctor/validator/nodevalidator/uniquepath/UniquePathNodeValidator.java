package me.myklebust.xpdoctor.validator.nodevalidator.uniquepath;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import me.myklebust.xpdoctor.validator.Validator;
import me.myklebust.xpdoctor.validator.ValidatorResults;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.repository.RepositoryService;
import com.enonic.xp.task.ProgressReporter;

@Component(immediate = true)
public class UniquePathNodeValidator
    implements Validator
{
    private RepositoryService repositoryService;

    private NodeService nodeService;

    @Override
    public String name()
    {
        return "Unique path validator";
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
        return new UniquePathValidatorExecutor( this.nodeService, this.repositoryService, reporter ).execute();
    }

    @Override
    public boolean repair( final NodeId nodeId )
    {
        return false;
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
}
