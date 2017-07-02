package me.myklebust.xpdoctor.validator.nodevalidator.uniquepath;

import me.myklebust.xpdoctor.validator.ValidatorResults;
import me.myklebust.xpdoctor.validator.nodevalidator.AbstractNodeValidator;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.repository.RepositoryService;

public class UniquePathNodeValidator
    extends AbstractNodeValidator
{
    private final RepositoryService repositoryService;

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

    public UniquePathNodeValidator( final NodeService nodeService, final RepositoryService repositoryService )
    {
        super( nodeService );
        this.repositoryService = repositoryService;
    }

    @Override
    public ValidatorResults validate()
    {
        return new UniquePathValidatorExecutor( this.nodeService, this.repositoryService ).execute();
    }

    @Override
    public boolean repair( final NodeId nodeId )
    {
        return false;
    }
}
