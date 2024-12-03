package me.myklebust.xpdoctor.validator.nodevalidator.inherit;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.ValidatorResult;
import me.myklebust.xpdoctor.validator.ValidatorResultImpl;
import me.myklebust.xpdoctor.validator.ValidatorResults;
import me.myklebust.xpdoctor.validator.nodevalidator.AbstractNodeExecutor;
import me.myklebust.xpdoctor.validator.nodevalidator.BatchedQueryExecutor;

import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeIds;
import com.enonic.xp.node.NodeService;


public class InheritFieldExecutor
    extends AbstractNodeExecutor
{
    public static final int BATCH_SIZE = 1_000;

    private final NodeService nodeService;

    private IndexValueService indexValueService;


    private final Logger LOG = LoggerFactory.getLogger( InheritFieldExecutor.class );

    private InheritFieldExecutor( final Builder builder )
    {
        super( builder );
        nodeService = builder.nodeService;
        indexValueService = builder.indexValueService;
    }

    public static Builder create()
    {
        return new Builder();
    }


    public ValidatorResults execute()
    {
        LOG.info( "Running InheritFieldExecutor..." );

        reportStart();

        final BatchedQueryExecutor executor = BatchedQueryExecutor.create().batchSize( BATCH_SIZE ).nodeService( this.nodeService ).build();

        final ValidatorResults.Builder results = ValidatorResults.create();

        int execute = 0;

        while ( executor.hasMore() )
        {
            LOG.info( "Checking nodes " + execute + "->" + ( execute + BATCH_SIZE ) + " of " + executor.getTotalHits() );
            reportProgress( executor.getTotalHits(), execute );

            final NodeIds nodesToCheck = executor.execute();
            results.add( checkNodes( nodesToCheck ) );
            execute += BATCH_SIZE;
        }

        return results.build();
    }

    private List<ValidatorResult> checkNodes( final NodeIds nodeIds )
    {
        List<ValidatorResult> results = Lists.newArrayList();

        for ( final NodeId nodeId : nodeIds )
        {
            try
            {
                final ValidatorResult result = doCheckNode( nodeId );

                if ( result != null )
                {
                    results.add( result );
                }

            }
            catch ( Exception e )
            {
                LOG.error( "Cannot check 'inherit' field for node with id: " + nodeId + "", e );
            }
        }
        return results;
    }

    private ValidatorResult doCheckNode( final NodeId nodeId )
    {

        final com.enonic.xp.node.Node node = nodeService.getById( nodeId );
        final Set<String> inheritFromBlobs = extractInherit( node.data() );

        final Set<String> docValues = indexValueService.getFieldsValue( nodeId, "search", InheritFieldValidator.FIELDS_TO_VALIDATE );

        if ( docValues != null && !inheritFromBlobs.equals( docValues ) )
        {
            // broken blob
            return ValidatorResultImpl.create()
                .nodeId( nodeId )
                .nodePath( node.path() )
                .nodeVersionId( node.getNodeVersionId() )
                .timestamp( node.getTimestamp() )
                .type( "Broken inherit" )
                .validatorName( validatorName )
                .message( "Node with id : " + node.id() + " has inconsistency between index and blob 'inherit' field value" )
                .repairResult( RepairResult.create()
                                   .message( "Field can be fixed in blob according to search index" )
                                   .repairStatus( RepairStatus.IS_REPAIRABLE )
                                   .build() )
                .build();
        }

        return null;

    }

    private Set<String> extractInherit( final PropertyTree nodeData )
    {
        return StreamSupport.stream( nodeData.getStrings( "inherit" ).spliterator(), false ).collect( Collectors.toSet() );
    }

    public static final class Builder
        extends AbstractNodeExecutor.Builder<Builder>
    {
        private NodeService nodeService;

        private IndexValueService indexValueService;

        private Builder()
        {
        }

        public Builder nodeService( final NodeService val )
        {
            nodeService = val;
            return this;
        }

        public Builder indexValueResolver( final IndexValueService val )
        {
            indexValueService = val;
            return this;
        }

        public InheritFieldExecutor build()
        {
            return new InheritFieldExecutor( this );
        }
    }
}
