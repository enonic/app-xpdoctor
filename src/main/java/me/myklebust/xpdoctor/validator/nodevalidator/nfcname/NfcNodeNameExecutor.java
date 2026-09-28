package me.myklebust.xpdoctor.validator.nodevalidator.nfcname;

import java.text.Normalizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.StorageSpyService;
import me.myklebust.xpdoctor.validator.ValidatorResult;
import me.myklebust.xpdoctor.validator.nodevalidator.Reporter;
import me.myklebust.xpdoctor.validator.nodevalidator.ScrollQueryExecutor;

import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeIds;
import com.enonic.xp.node.NodeService;

public class NfcNodeNameExecutor
{
    private static final Logger LOG = LoggerFactory.getLogger( NfcNodeNameExecutor.class );

    private final NodeService nodeService;

    private final StorageSpyService storageSpyService;

    public NfcNodeNameExecutor( final NodeService nodeService, final StorageSpyService storageSpyService )
    {
        this.nodeService = nodeService;
        this.storageSpyService = storageSpyService;
    }

    public void execute( final Reporter reporter )
    {
        LOG.info( "Running NfcNodeNameExecutor..." );
        reporter.reportStart();

        ScrollQueryExecutor.create()
            .progressReporter( reporter.getProgressReporter() )
            .indexType( ScrollQueryExecutor.IndexType.STORAGE )
            .spyStorageService( this.storageSpyService )
            .build()
            .execute( nodesToCheck -> checkNodes( nodesToCheck, reporter ) );

        LOG.info( "... NfcNodeNameExecutor done" );
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
                LOG.error( "Cannot check name normalization for node with id: {}", nodeId, e );
            }
        }
    }

    private void doCheckNode( final Reporter results, final NodeId nodeId )
    {
        final Node node = this.nodeService.getById( nodeId );
        if ( node == null || node.isRoot() )
        {
            return;
        }

        final String name = node.name().toString();
        if ( Normalizer.isNormalized( name, Normalizer.Form.NFC ) )
        {
            return;
        }

        results.addResult( ValidatorResult.create()
                               .nodeId( nodeId )
                               .nodePath( node.path() )
                               .nodeVersionId( node.getNodeVersionId() )
                               .timestamp( node.getTimestamp() )
                               .type( "Not NFC name" )
                               .validatorName( results.validatorName )
                               .message( "Node name is not Unicode NFC normalized" )
                               .repairResult( RepairResult.create()
                                                  .message( "Rename node to [" + Normalizer.normalize( name, Normalizer.Form.NFC ) + "]" )
                                                  .repairStatus( RepairStatus.IS_REPAIRABLE )
                                                  .build() ) );
    }
}
