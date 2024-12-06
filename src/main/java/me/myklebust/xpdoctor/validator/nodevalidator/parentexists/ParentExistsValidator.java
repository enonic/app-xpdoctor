package me.myklebust.xpdoctor.validator.nodevalidator.parentexists;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.Validator;
import me.myklebust.xpdoctor.validator.ValidatorResults;
import me.myklebust.xpdoctor.validator.nodevalidator.Reporter;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.task.ProgressReporter;

@Component(immediate = true)
public class ParentExistsValidator
    implements Validator
{
    @Reference
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
        final Reporter results = new Reporter( name(), reporter );
        new ParentExistsExistsExecutor( nodeService ).execute( results );
        return results.buildResults();
    }

    @Override
    public RepairResult repair( final NodeId nodeId )
    {
        return this.doctor.repairNode( nodeId, false );
    }
}
