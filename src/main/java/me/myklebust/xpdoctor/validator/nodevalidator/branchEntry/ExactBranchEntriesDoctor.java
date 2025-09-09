package me.myklebust.xpdoctor.validator.nodevalidator.branchEntry;

import com.enonic.xp.content.ContentConstants;
import com.enonic.xp.node.*;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.nodevalidator.NodeDoctor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExactBranchEntriesDoctor
    implements NodeDoctor
{
    private static final Logger LOG = LoggerFactory.getLogger( ExactBranchEntriesDoctor.class );

    private final NodeService nodeService;

    public ExactBranchEntriesDoctor( final NodeService nodeService )
    {
        this.nodeService = nodeService;
    }

    public RepairResult repairNode( final NodeId nodeId, final boolean dryRun )
    {
        LOG.info( "Trying to repair node with equal branch entries, nodeId: {}", nodeId );
        this.nodeService.refresh( RefreshMode.ALL );

        try
        {
            final PushNodesResult result = this.nodeService.push( NodeIds.from( nodeId ), ContentConstants.BRANCH_MASTER );

            if ( result.getSuccessful().isNotEmpty() )
            {
                return RepairResult.create()
                    .repairStatus( RepairStatus.REPAIRED )
                    .message( String.format( "Node with id: %s pushed to master", nodeId ) )
                    .build();
            }
            else
            {
                return RepairResult.create()
                    .repairStatus( RepairStatus.FAILED )
                    .message( String.format( "Node with id: %s could not be pushed to master. %s", nodeId, result.getFailed()
                        .stream()
                        .findFirst()
                        .map( f -> f.getReason().toString() )
                        .orElse( "No details available" ) ) )
                    .build();
            }
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to repair node", e );

            return RepairResult.create()
                .message( "Cannot repair node, exception when trying to push: " + e.getMessage() )
                .repairStatus( RepairStatus.FAILED )
                .build();
        }


    }
}

