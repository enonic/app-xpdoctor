package me.myklebust.xpdoctor.validator.nodevalidator.blobmissing;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import me.myklebust.xpdoctor.filespy.FileBlobStoreSpyService;
import me.myklebust.xpdoctor.storagespy.StorageSpyService;
import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.Validator;
import me.myklebust.xpdoctor.validator.ValidatorResults;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.task.ProgressReporter;

@Component(immediate = true)
public class BlobMissingValidator implements Validator
{
    @Reference
    private NodeService nodeService;

    @Reference
    private StorageSpyService storageSpyService;

    @Reference
    private FileBlobStoreSpyService fileBlobStoreSpyService;

    private BlobMissingDoctor doctor;

    public BlobMissingValidator()
    {
        this.doctor = new BlobMissingDoctor();
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
        return BlobMissingExecutor.create()
            .nodeService( this.nodeService )
            .progressReporter( reporter )
            .validatorName( name() )
            .doctor( this.doctor )
            .storageSpyService( this.storageSpyService )
            .blobStore( this.fileBlobStoreSpyService.getBlobStore() )
            .build()
            .execute();
    }

    @Override
    public RepairResult repair( final NodeId nodeId )
    {
        return doctor.repairBlob( nodeId, false );
    }

    @Override
    public int order()
    {
        return 5;
    }
}
