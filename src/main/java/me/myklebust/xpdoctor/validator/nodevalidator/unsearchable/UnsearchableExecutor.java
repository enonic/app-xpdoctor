package me.myklebust.xpdoctor.validator.nodevalidator.unsearchable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.StorageSpyService;
import me.myklebust.xpdoctor.validator.ValidatorResult;
import me.myklebust.xpdoctor.validator.nodevalidator.Reporter;
import me.myklebust.xpdoctor.validator.nodevalidator.ScrollQueryExecutor;

import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.node.GetNodeVersionsParams;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeIds;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.node.NodeVersionMetadata;
import com.enonic.xp.node.NodeVersionQueryResult;

public class UnsearchableExecutor
{
    private static final Logger LOG = LoggerFactory.getLogger( UnsearchableExecutor.class );

    private final StorageSpyService storageSpyService;

    public UnsearchableExecutor( final StorageSpyService storageSpyService )
    {
        this.storageSpyService = storageSpyService;
    }

    public void execute( final Reporter reporter )
    {
        LOG.info( "Running UnsearchableExecutor..." );
        reporter.reportStart();

        ScrollQueryExecutor.create()
            .progressReporter( reporter.getProgressReporter() )
            .indexType( ScrollQueryExecutor.IndexType.STORAGE )
            .spyStorageService( this.storageSpyService )
            .build()
            .execute( nodesToCheck -> checkNodes( nodesToCheck, reporter ) );

        LOG.info( "... UnsearchableExecutor done" );
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
                LOG.error( "Cannot check node with id: " + nodeId, e );
            }
        }
    }

    private void doCheckNode( final Reporter results, final NodeId nodeId )
    {
        boolean inSearch =
            storageSpyService.existsInSearch( nodeId, ContextAccessor.current().getRepositoryId(), ContextAccessor.current().getBranch() );
        if (!inSearch)
        {
            results.addResult( ValidatorResult.create()
                                   .nodeId( nodeId )
                                   .type( "Unsearchable node" )
                                   .message( "Node is not searchable" )
                                   .repairResult( RepairResult.create()
                                                      .message( "Non repairable automatically. Reindex search" )
                                                      .repairStatus( RepairStatus.NOT_REPAIRABLE )
                                                      .build() ) );
        }
    }
}
