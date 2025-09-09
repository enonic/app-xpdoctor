package me.myklebust.xpdoctor.validator.nodevalidator.branchEntry;

import me.myklebust.xpdoctor.validator.nodevalidator.parentexists.NoParentDoctor;

import org.osgi.service.component.annotations.Activate;
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
public class ExactBranchEntriesValidator
    implements Validator
{
    @Reference
    private NodeService nodeService;

    @Reference
    private StorageSpyService storageSpyService;

    private ExactBranchEntriesDoctor doctor;


    @Activate
    public void activate()
    {
        this.doctor = new ExactBranchEntriesDoctor( this.nodeService );
    }

    @Override
    public int order()
    {
        return 7;
    }

    @Override
    public String getDescription()
    {
        return "Validates that exact equal Branch Entries point to one version";
    }

    @Override
    public String getRepairStrategy()
    {
        return "Push draft to master";
    }

    @Override
    public ValidatorResults validate( final ProgressReporter reporter )
    {
        final Reporter results = new Reporter( name(), reporter );
        new ExactBranchEntriesExecutor( nodeService, storageSpyService ).execute( results );
        return results.buildResults();
    }

    @Override
    public RepairResult repair( final NodeId nodeId )
    {
        return doctor.repairNode( nodeId, false );
    }
}
