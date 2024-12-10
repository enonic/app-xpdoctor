package me.myklebust.xpdoctor.validator.nodevalidator.unloadable;

import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.StorageSpyService;
import me.myklebust.xpdoctor.validator.ValidatorResult;
import me.myklebust.xpdoctor.validator.nodevalidator.BatchedQueryExecutor;
import me.myklebust.xpdoctor.validator.nodevalidator.Reporter;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeIds;
import com.enonic.xp.node.NodeService;

public class LoadableNodeExecutor
{
    private static final Logger LOG = LoggerFactory.getLogger( LoadableNodeExecutor.class );

    private static final String TYPE = "Unloadable node";

    private final NodeService nodeService;

    private final StorageSpyService storageSpyService;

    private final LoadableNodeDoctor doctor;

    public LoadableNodeExecutor( final NodeService nodeService, final LoadableNodeDoctor doctor, final StorageSpyService storageSpyService )
    {
        this.nodeService = nodeService;
        this.doctor = doctor;
        this.storageSpyService = storageSpyService;
    }

    public void execute( final Reporter reporter )
    {
        LOG.info( "Running LoadableNodeExecutor..." );

        BatchedQueryExecutor.create()
            .progressReporter( reporter.getProgressReporter() )
            .spyStorageService( storageSpyService )
            .build()
            .execute( nodesToCheck -> checkNodes( nodesToCheck, reporter ) );

        LOG.info( "... LoadableNodeExecutor done" );
    }

    private void checkNodes( final NodeIds nodeIds, final Reporter results )
    {
        for ( final NodeId nodeId : nodeIds )
        {
            doCheckNode( results, nodeId );
        }
    }

    private void doCheckNode( final Reporter results, final NodeId nodeId )
    {
        try
        {
            this.nodeService.getById( nodeId );
        }
        catch ( Exception e )
        {
            resolveAndRepair( results, nodeId, e.getMessage() );
        }
    }

    private void resolveAndRepair( Reporter results, final NodeId nodeId, final String message )
    {
        try
        {
            final RepairResult repairResult = this.doctor.repairNode( nodeId, true );

            results.addResult( ValidatorResult.create()
                                   .nodeId( nodeId )
                                   .nodePath( null )
                                   .nodeVersionId( null )
                                   .timestamp( null )
                                   .type( TYPE )
                                   .validatorName( results.validatorName )
                                   .message( message )
                                   .repairResult( repairResult )
                                   .build() );
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to repair", e );
        }
    }
}
