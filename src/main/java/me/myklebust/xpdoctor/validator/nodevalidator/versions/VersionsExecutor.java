package me.myklebust.xpdoctor.validator.nodevalidator.versions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.StorageSpyService;
import me.myklebust.xpdoctor.validator.ValidatorResult;
import me.myklebust.xpdoctor.validator.nodevalidator.BatchedQueryExecutor;
import me.myklebust.xpdoctor.validator.nodevalidator.Reporter;

import com.enonic.xp.node.GetNodeVersionsParams;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeIds;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.node.NodeVersionMetadata;
import com.enonic.xp.node.NodeVersionQueryResult;

public class VersionsExecutor
{
    private static final Logger LOG = LoggerFactory.getLogger( VersionsExecutor.class );

    private final NodeService nodeService;

    private final StorageSpyService storageSpyService;

    public VersionsExecutor( final NodeService nodeService, final StorageSpyService storageSpyService )
    {
        this.nodeService = nodeService;
        this.storageSpyService = storageSpyService;
    }

    public void execute( final Reporter reporter )
    {
        LOG.info( "Running VersionsExecutor..." );
        reporter.reportStart();

        BatchedQueryExecutor.create()
            .progressReporter( reporter.getProgressReporter() )
            .spyStorageService( this.storageSpyService )
            .build()
            .execute( nodesToCheck -> checkNodes( nodesToCheck, reporter ) );

        LOG.info( "... VersionsExecutor done" );
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
        final NodeVersionQueryResult versions =
            nodeService.findVersions( GetNodeVersionsParams.create().nodeId( nodeId ).size( -1 ).build() );
        for ( NodeVersionMetadata nodeVersionsMetadata : versions.getNodeVersionsMetadata() )
        {
            try
            {
                nodeService.getByNodeVersionKey( nodeVersionsMetadata.getNodeVersionKey() );
            }
            catch ( Exception e )
            {
                results.addResult( ValidatorResult.create()
                                       .nodeId( nodeId )
                                       .nodePath( nodeVersionsMetadata.getNodePath() )
                                       .nodeVersionId( nodeVersionsMetadata.getNodeVersionId() )
                                       .timestamp( nodeVersionsMetadata.getTimestamp() )
                                       .type( "Unloadable Version" )
                                       .message( "Cannot load version data" )
                                       .repairResult( RepairResult.create()
                                                          .message( "Non repairable automatically" )
                                                          .repairStatus( RepairStatus.NOT_REPAIRABLE )
                                                          .build() ) );
            }
        }
    }
}
