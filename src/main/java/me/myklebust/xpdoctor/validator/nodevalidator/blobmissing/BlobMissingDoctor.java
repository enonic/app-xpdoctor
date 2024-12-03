package me.myklebust.xpdoctor.validator.nodevalidator.blobmissing;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepairStatus;

import com.enonic.xp.node.NodeId;

public class BlobMissingDoctor
{
    RepairResult repairBlob( final NodeId node, final boolean dryRun )
    {
        return RepairResult.create().message( "Blob missing" ).repairStatus( RepairStatus.NOT_REPAIRABLE ).build();
    }
}
