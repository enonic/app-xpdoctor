package me.myklebust.xpdoctor.validator.nodevalidator.unsearchable;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.StorageSpyService;
import me.myklebust.xpdoctor.validator.Validator;
import me.myklebust.xpdoctor.validator.ValidatorResults;
import me.myklebust.xpdoctor.validator.nodevalidator.Reporter;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.task.ProgressReporter;

@Component(immediate = true)
public class UnsearchableValidator
    implements Validator
{
    @Reference
    private NodeService nodeService;

    @Reference
    private StorageSpyService storageSpyService;

    @Override
    public int order()
    {
        return 6;
    }

    @Override
    public String getDescription()
    {
        return "Validates that node is searchable";
    }

    @Override
    public String getRepairStrategy()
    {
        return "";
    }

    @Override
    public ValidatorResults validate( final ProgressReporter reporter )
    {
        final Reporter results = new Reporter( name(), reporter );
        new UnsearchableExecutor( nodeService, storageSpyService ).execute( results );
        return results.buildResults();
    }

    @Override
    public RepairResult repair( final NodeId nodeId )
    {
        return RepairResult.create()
            .repairStatus( RepairStatus.NOT_REPAIRABLE )
            .message( "cannot automatically node searchable" )
            .build();
    }
}
