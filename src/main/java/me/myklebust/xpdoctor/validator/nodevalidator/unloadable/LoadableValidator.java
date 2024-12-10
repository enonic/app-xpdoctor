package me.myklebust.xpdoctor.validator.nodevalidator.unloadable;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.StorageSpyService;
import me.myklebust.xpdoctor.validator.Validator;
import me.myklebust.xpdoctor.validator.ValidatorResults;
import me.myklebust.xpdoctor.validator.nodevalidator.Reporter;

import com.enonic.xp.blob.BlobStore;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.task.ProgressReporter;

@Component(immediate = true)
public class LoadableValidator
    implements Validator
{
    @Reference
    private NodeService nodeService;

    @Reference
    private BlobStore blobStore;

    @Reference
    private StorageSpyService storageSpyService;

    private LoadableNodeDoctor doctor;

    @Override
    public int order()
    {
        return 0;
    }

    @Activate
    public void activate()
    {
        this.doctor = new LoadableNodeDoctor( this.nodeService, this.blobStore, this.storageSpyService );
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
        final Reporter results = new Reporter( name(), reporter );
        new LoadableNodeExecutor( nodeService, doctor, storageSpyService ).execute( results );
        return results.buildResults();
    }

    @Override
    public RepairResult repair( final NodeId nodeId )
    {
        return this.doctor.repairNode( nodeId, false );
    }
}
