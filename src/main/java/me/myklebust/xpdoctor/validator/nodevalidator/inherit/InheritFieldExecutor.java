package me.myklebust.xpdoctor.validator.nodevalidator.inherit;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.ValidatorResult;
import me.myklebust.xpdoctor.validator.nodevalidator.BatchedQueryExecutor;
import me.myklebust.xpdoctor.validator.nodevalidator.Reporter;

import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeIds;
import com.enonic.xp.node.NodeService;


public class InheritFieldExecutor
{
    private static final Logger LOG = LoggerFactory.getLogger( InheritFieldExecutor.class );

    private final NodeService nodeService;

    private IndexValueService indexValueService;

    public InheritFieldExecutor( final NodeService nodeService, final IndexValueService indexValueService )
    {
        this.nodeService = nodeService;
        this.indexValueService = indexValueService;
    }

    public void execute( final Reporter reporter )
    {
        LOG.info( "Running InheritFieldExecutor..." );

        reporter.reportStart();

        BatchedQueryExecutor.create()
            .progressReporter( reporter.getProgressReporter() )
            .nodeService( this.nodeService )
            .build()
            .execute( nodesToCheck -> checkNodes( nodesToCheck, reporter ) );

        LOG.info( "... InheritFieldExecutor done" );
    }

    private void checkNodes( final NodeIds nodeIds, final Reporter reporter )
    {
        for ( final NodeId nodeId : nodeIds )
        {
            try
            {
                doCheckNode( nodeId, reporter );
            }
            catch ( Exception e )
            {
                LOG.error( "Cannot check 'inherit' field for node with id: {}", nodeId, e );
            }
        }
    }

    private void doCheckNode( final NodeId nodeId, final Reporter reporter )
    {

        final com.enonic.xp.node.Node node = nodeService.getById( nodeId );
        final Set<String> inheritFromBlobs = extractInherit( node.data() );

        final Set<String> docValues = indexValueService.getFieldsValue( nodeId, "search", InheritFieldValidator.FIELDS_TO_VALIDATE );

        if ( docValues != null && !inheritFromBlobs.equals( docValues ) )
        {
            reporter.addResult( ValidatorResult.create()
                                    .nodeId( nodeId )
                                    .nodePath( node.path() )
                                    .nodeVersionId( node.getNodeVersionId() )
                                    .timestamp( node.getTimestamp() )
                                    .type( "Broken inherit" )
                                    .validatorName( reporter.validatorName )
                                    .message(
                                        "Node with id : " + node.id() + " has inconsistency between index and blob 'inherit' field value" )
                                    .repairResult( RepairResult.create()
                                                       .message( "Field can be fixed in blob according to search index" )
                                                       .repairStatus( RepairStatus.IS_REPAIRABLE )
                                                       .build() )
                                    .build() );
        }
    }

    private Set<String> extractInherit( final PropertyTree nodeData )
    {
        return StreamSupport.stream( nodeData.getStrings( "inherit" ).spliterator(), false ).collect( Collectors.toSet() );
    }
}
