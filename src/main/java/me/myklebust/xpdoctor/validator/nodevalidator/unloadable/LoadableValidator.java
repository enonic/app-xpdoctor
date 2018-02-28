package me.myklebust.xpdoctor.validator.nodevalidator.unloadable;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.Validator;
import me.myklebust.xpdoctor.validator.ValidatorResults;

import com.enonic.xp.blob.BlobStore;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.task.ProgressReporter;

@Component(immediate = true)
public class LoadableValidator
    implements Validator
{
    private NodeService nodeService;

    private LoadableNodeDoctor doctor;

    private BlobStore blobStore;

    @Override
    public int order()
    {
        return 0;
    }

    @Activate
    public void activate()
    {
        this.doctor = new LoadableNodeDoctor( this.nodeService, this.blobStore );
    }

    @Override
    public String name()
    {
        return this.getClass().getSimpleName();
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
        return LoadableNodeExecutor.create().
            nodeService( this.nodeService ).
            doctor( this.doctor ).
            progressReporter( reporter ).
            validatorName( this.name() ).
            build().
            execute();
    }

    @Override
    public RepairResult repair( final NodeId nodeId )
    {
        return this.doctor.repaidNode( nodeId, true );
    }

    @Reference
    public void setNodeService( final NodeService nodeService )
    {
        this.nodeService = nodeService;
    }

    @Reference
    public void setBlobStore( final BlobStore blobStore )
    {
        this.blobStore = blobStore;
    }

    @Override
    public int compareTo( final Validator o )
    {
        return Integer.compare( this.order(), o.order() );
    }

}
