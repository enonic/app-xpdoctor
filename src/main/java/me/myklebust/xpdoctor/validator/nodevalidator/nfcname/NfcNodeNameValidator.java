package me.myklebust.xpdoctor.validator.nodevalidator.nfcname;

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
public class NfcNodeNameValidator
    implements Validator
{
    @Reference
    private NodeService nodeService;

    @Reference
    private StorageSpyService storageSpyService;

    private NfcNodeNameDoctor doctor;

    @Activate
    public void activate()
    {
        this.doctor = new NfcNodeNameDoctor( this.nodeService );
    }

    @Override
    public int order()
    {
        return 8;
    }

    @Override
    public String getDescription()
    {
        return "Validates that node names are valid in XP 8: Unicode NFC normalized, with no characters XP 8 does not allow";
    }

    @Override
    public String getRepairStrategy()
    {
        return "Rename node to the NFC normalized name in the repaired branch. Names still invalid after normalization must be renamed manually";
    }

    @Override
    public ValidatorResults validate( final ProgressReporter reporter )
    {
        final Reporter results = new Reporter( name(), reporter );
        new NfcNodeNameExecutor( nodeService, storageSpyService, doctor ).execute( results );
        return results.buildResults();
    }

    @Override
    public RepairResult repair( final NodeId nodeId )
    {
        return this.doctor.repairNode( nodeId, false );
    }
}
