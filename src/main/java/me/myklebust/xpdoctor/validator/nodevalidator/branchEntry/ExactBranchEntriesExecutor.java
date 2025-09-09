package me.myklebust.xpdoctor.validator.nodevalidator.branchEntry;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.branch.Branches;
import com.enonic.xp.content.ContentConstants;
import com.enonic.xp.node.*;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.StorageSpyService;
import me.myklebust.xpdoctor.validator.ValidatorResult;
import me.myklebust.xpdoctor.validator.nodevalidator.Reporter;
import me.myklebust.xpdoctor.validator.nodevalidator.ScrollQueryExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ExactBranchEntriesExecutor
{
    private static final Logger LOG = LoggerFactory.getLogger( ExactBranchEntriesExecutor.class );

    private final NodeService nodeService;

    private final StorageSpyService storageSpyService;

    public ExactBranchEntriesExecutor( final NodeService nodeService, final StorageSpyService storageSpyService )
    {
        this.nodeService = nodeService;
        this.storageSpyService = storageSpyService;
    }

    public void execute( final Reporter reporter )
    {
        LOG.info( "Running ExactBranchEntriesExecutor..." );
        reporter.reportStart();

        ScrollQueryExecutor.create()
            .progressReporter( reporter.getProgressReporter() )
            .indexType( ScrollQueryExecutor.IndexType.STORAGE )
            .spyStorageService( this.storageSpyService )

            .build()
            .execute( nodesToCheck -> checkNodes( nodesToCheck, reporter ) );

        LOG.info( "... ExactBranchEntriesExecutor done" );
    }

    private void checkNodes( final NodeIds nodeIds, final Reporter results )
    {
        for ( final NodeId nodeId : nodeIds )
        {
            try
            {
                doCheckNode( results, nodeId );
            }
            catch ( Exception e )
            {
                LOG.error( "Cannot check versions node with id: " + nodeId, e );
            }
        }
    }

    private void doCheckNode( final Reporter results, final NodeId nodeId )
    {
        final Map<Branch, NodeVersionMetadata> versions = nodeService.getActiveVersions( GetActiveNodeVersionsParams.create()
                                                                                             .nodeId( nodeId )
                                                                                             .branches( Branches.from(
                                                                                                 ContentConstants.BRANCH_DRAFT,
                                                                                                 ContentConstants.BRANCH_MASTER ) )
                                                                                             .build() ).getNodeVersions();

        final NodeVersionMetadata draft = versions.get( ContentConstants.BRANCH_DRAFT );
        final NodeVersionMetadata master = versions.get( ContentConstants.BRANCH_MASTER );

        if ( draft != null && master != null )
        {
            if ( areEntriesExact( draft, master ) && areVersionIdDifferent( draft, master ) )
            {
                results.addResult( ValidatorResult.create()
                                       .nodeId( nodeId )
                                       .nodePath( draft.getNodePath() )
                                       .nodeVersionId( master.getNodeVersionId() )
                                       .timestamp( draft.getTimestamp() )
                                       .type( "Exact branch entries, different versions" )
                                       .validatorName( results.validatorName )
                                       .message( String.format(
                                           "Exact the same active versions for the node, but version ids are different: draft: %s, master: %s",
                                           draft.getNodeVersionId(), master.getNodeVersionId() ) )
                                       .repairResult( RepairResult.create()
                                                          .message(
                                                              String.format( "Push draft version %s to master", draft.getNodeVersionId() ) )
                                                          .repairStatus( RepairStatus.IS_REPAIRABLE )
                                                          .build() ) );
            }
        }
    }

    private boolean areEntriesExact( NodeVersionMetadata draft, NodeVersionMetadata master )
    {
        return draft.getNodeId().equals( master.getNodeId() ) && draft.getBinaryBlobKeys().equals( master.getBinaryBlobKeys() ) &&
            draft.getNodePath().equals( master.getNodePath() ) && draft.getTimestamp().equals( master.getTimestamp() ) &&
            draft.getNodeVersionKey().equals( master.getNodeVersionKey() );
    }

    private boolean areVersionIdDifferent( NodeVersionMetadata draft, NodeVersionMetadata master )
    {
        return !draft.getNodeVersionId().equals( master.getNodeVersionId() );
    }
}
