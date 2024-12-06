package me.myklebust.xpdoctor.validator.nodevalidator.inherit;

import java.util.Set;

import org.elasticsearch.client.Client;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.Validator;
import me.myklebust.xpdoctor.validator.ValidatorResults;
import me.myklebust.xpdoctor.validator.nodevalidator.Reporter;

import com.enonic.xp.blob.BlobStore;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.task.ProgressReporter;

@Component(immediate = true)
public class InheritFieldValidator
    implements Validator
{
    @Reference
    private NodeService nodeService;

    @Reference
    private Client client;

    @Reference
    private BlobStore blobStore;

    private InheritFieldDoctor doctor;

    public static final Set<String> FIELDS_TO_VALIDATE = Set.of( "inherit", "inherit._analyzed", "inherit._ngram" );

    @Activate
    public void activate()
    {
        this.doctor = InheritFieldDoctor.create()
            .blobStore( blobStore )
            .indexValueResolver( new IndexValueService( client ) )
            .nodeService( nodeService )
            .build();
    }

    @Override
    public int order()
    {
        return 3;
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
        final Reporter results = new Reporter( name(), reporter );
        new InheritFieldExecutor( nodeService, new IndexValueService( client ) ).execute( results );
        return results.buildResults();
    }

    @Override
    public RepairResult repair( final NodeId nodeId )
    {
        return this.doctor.repairNode( nodeId, false );
    }
}
