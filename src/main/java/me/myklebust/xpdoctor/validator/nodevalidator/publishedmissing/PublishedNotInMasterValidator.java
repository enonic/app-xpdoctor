package me.myklebust.xpdoctor.validator.nodevalidator.publishedmissing;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.StorageSpyService;
import me.myklebust.xpdoctor.validator.Validator;
import me.myklebust.xpdoctor.validator.ValidatorResults;
import me.myklebust.xpdoctor.validator.nodevalidator.Reporter;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.task.ProgressReporter;

@Component(immediate = true)
public class PublishedNotInMasterValidator
    implements Validator
{
    @Reference
    private NodeService nodeService;

    @Reference
    private StorageSpyService storageSpyService;

    private PublishedNotInMasterDoctor doctor;

    @Activate
    public void activate()
    {
        this.doctor = new PublishedNotInMasterDoctor( this.nodeService );
    }

    @Override
    public int order()
    {
        return 9;
    }

    @Override
    public String getDescription()
    {
        return "Validates that content published according to draft exists in master";
    }

    @Override
    public String getRepairStrategy()
    {
        return "Push the current draft version to master";
    }

    @Override
    public ValidatorResults validate( final ProgressReporter reporter )
    {
        final Reporter results = new Reporter( name(), reporter );
        new PublishedNotInMasterExecutor( nodeService, storageSpyService, doctor ).execute( results );
        return results.buildResults();
    }

    @Override
    public RepairResult repair( final NodeId nodeId )
    {
        return this.doctor.repairNode( nodeId, false );
    }
}
