package me.myklebust.xpdoctor.validator.nodevalidator.parentexists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.StorageSpyService;
import me.myklebust.xpdoctor.validator.ValidatorResult;
import me.myklebust.xpdoctor.validator.nodevalidator.BatchedQueryExecutor;
import me.myklebust.xpdoctor.validator.nodevalidator.Reporter;

import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeIds;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.node.NodeService;

public class ParentExistsExistsExecutor
{
    private static final Logger LOG = LoggerFactory.getLogger( ParentExistsExistsExecutor.class );

    private final NodeService nodeService;

    private final StorageSpyService storageSpyService;

    public ParentExistsExistsExecutor( final NodeService nodeService, final StorageSpyService storageSpyService )
    {
        this.nodeService = nodeService;
        this.storageSpyService = storageSpyService;
    }

    public void execute( final Reporter reporter )
    {
        LOG.info( "Running LoadableNodeExecutor..." );
        reporter.reportStart();

        BatchedQueryExecutor.create()
            .progressReporter( reporter.getProgressReporter() )
            .spyStorageService( this.storageSpyService )
            .build()
            .execute( nodesToCheck -> checkNodes( nodesToCheck, reporter ) );

        LOG.info( "... LoadableNodeExecutor done" );
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
                LOG.error( "Cannot check parent exists for node with id: {}", nodeId, e );
            }
        }

    }

    private void doCheckNode( final Reporter results, final NodeId nodeId )
    {
        final Node node = this.nodeService.getById( nodeId );

        final NodePath parentPath = node.path().getParentPath();

        final Node parent = this.nodeService.getByPath( parentPath );

        if ( parent == null )
        {
            results.addResult( ValidatorResult.create()
                                   .nodeId( nodeId )
                                   .nodePath( node.path() )
                                   .nodeVersionId( node.getNodeVersionId() )
                                   .timestamp( node.getTimestamp() )
                                   .type( "No parent" )
                                   .validatorName( results.validatorName )
                                   .message( "Parent with path : " + node.parentPath() + " not found" )
                                   .repairResult( RepairResult.create()
                                                      .message( "Create parent with path [" + node.parentPath() + "]" )
                                                      .repairStatus( RepairStatus.MANUAL )
                                                      .build() )
                                   .build() );
        }
    }
}
