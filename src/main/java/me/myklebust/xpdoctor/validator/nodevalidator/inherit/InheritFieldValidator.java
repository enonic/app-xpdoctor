package me.myklebust.xpdoctor.validator.nodevalidator.inherit;

import java.util.Set;

import org.elasticsearch.node.Node;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.Validator;
import me.myklebust.xpdoctor.validator.ValidatorResults;

import com.enonic.xp.blob.BlobStore;
import com.enonic.xp.blob.BlobStoreProvider;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.task.ProgressReporter;

@Component(immediate = true)
public class InheritFieldValidator
    implements Validator
{
    private NodeService nodeService;

    private Node node;

    private InheritFieldDoctor doctor;

    private BlobStore blobStore;

    public static final Set<String> FIELDS_TO_VALIDATE = Set.of( "inherit", "inherit._analyzed", "inherit._ngram" );

    @Activate
    public void activate()
    {
        this.doctor = InheritFieldDoctor.create()
            .blobStore( blobStore )
            .indexValueResolver( new IndexValueService( node ) )
            .nodeService( nodeService )
            .build();
    }

    @Override
    public int order()
    {
        return 3;
    }

    @Override
    public String name()
    {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getDescription()
    {
        return "Validates that 'inherit' node indices are equal to blobs";
    }

    @Override
    public String getRepairStrategy()
    {
        return "";
    }

    @Override
    public ValidatorResults validate( final ProgressReporter reporter )
    {
        return InheritFieldExecutor.create()
            .nodeService( this.nodeService )
            .indexValueResolver( new IndexValueService( this.node ) )
            .progressReporter( reporter )
            .validatorName( name() )
            .build()
            .execute();
    }

    @Override
    public RepairResult repair( final NodeId nodeId )
    {
        return this.doctor.repairNode( nodeId, true );
    }

    @Reference
    public void setNodeService( final NodeService nodeService )
    {
        this.nodeService = nodeService;
    }

    @Reference
    public void setNode( final Node node )
    {
        this.node = node;
    }

    @Reference
    public void setBlobStoreProvider( final BlobStoreProvider provider )
    {
        this.blobStore = provider.get();
    }

    @Override
    public int compareTo( final Validator o )
    {
        return Integer.compare( this.order(), o.order() );
    }
}
