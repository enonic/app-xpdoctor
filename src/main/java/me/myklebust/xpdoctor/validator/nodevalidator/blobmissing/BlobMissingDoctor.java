package me.myklebust.xpdoctor.validator.nodevalidator.blobmissing;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.nodevalidator.NodeDoctor;

import com.enonic.xp.node.NodeId;

public class BlobMissingDoctor
    implements NodeDoctor
{
    @Override
    public RepairResult repairNode( final NodeId nodeId, final boolean dryRun )
    {
        return RepairResult.create().message( "Blob missing" ).repairStatus( RepairStatus.NOT_REPAIRABLE ).build();
    }
}
