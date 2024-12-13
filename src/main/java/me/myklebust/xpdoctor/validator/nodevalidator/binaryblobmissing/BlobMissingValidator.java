package me.myklebust.xpdoctor.validator.nodevalidator.binaryblobmissing;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import me.myklebust.xpdoctor.validator.FileBlobStoreSpyService;
import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.StorageSpyService;
import me.myklebust.xpdoctor.validator.Validator;
import me.myklebust.xpdoctor.validator.ValidatorResults;
import me.myklebust.xpdoctor.validator.nodevalidator.Reporter;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.repository.RepositoryService;
import com.enonic.xp.task.ProgressReporter;

@Component(immediate = true)
public class BlobMissingValidator
    implements Validator
{
    @Reference
    private NodeService nodeService;

    @Reference
    private RepositoryService repositoryService;

    @Reference
    private StorageSpyService storageSpyService;

    @Reference
    private FileBlobStoreSpyService fileBlobStoreSpyService;

    private BlobMissingDoctor doctor;

    @Activate
    public void activate()
    {
        this.doctor = new BlobMissingDoctor( fileBlobStoreSpyService.getBlobStore(), repositoryService, storageSpyService );
    }

    @Override
    public String getDescription()
    {
        return "Validates that all blobs are present in the blob store.";
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
        new BlobMissingExecutor( storageSpyService, fileBlobStoreSpyService.getBlobStore(), doctor ).execute( results );
        return results.buildResults();
    }

    @Override
    public RepairResult repair( final NodeId nodeId )
    {
        return doctor.repairNode( nodeId, false );
    }

    @Override
    public int order()
    {
        return 5;
    }
}
