package me.myklebust.xpdoctor.validator.nodevalidator.versions;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.Validator;
import me.myklebust.xpdoctor.validator.ValidatorResults;
import me.myklebust.xpdoctor.validator.nodevalidator.Reporter;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.task.ProgressReporter;

@Component(immediate = true)
public class VersionsValidator
    implements Validator
{
    @Reference
    private NodeService nodeService;

    @Override
    public int order()
    {
        return 4;
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
        final Reporter results = new Reporter( name(), reporter );
        new VersionsExecutor( nodeService ).execute( results );
        return results.buildResults();
    }

    @Override
    public RepairResult repair( final NodeId nodeId )
    {
        return RepairResult.create()
            .repairStatus( RepairStatus.NOT_REPAIRABLE )
            .message( "cannot automatically fix broken versions" )
            .build();
    }
}
