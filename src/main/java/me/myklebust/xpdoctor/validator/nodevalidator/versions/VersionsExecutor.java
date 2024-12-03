package me.myklebust.xpdoctor.validator.nodevalidator.versions;

import java.util.List;

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

import com.enonic.xp.node.GetNodeVersionsParams;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeIds;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.node.NodeVersionMetadata;
import com.enonic.xp.node.NodeVersionQueryResult;

public class VersionsExecutor
    extends AbstractNodeExecutor
{
    public static final int BATCH_SIZE = 1_000;

    private final NodeService nodeService;

    private final Logger LOG = LoggerFactory.getLogger( VersionsExecutor.class );

    private VersionsExecutor( final Builder builder )
    {
        super( builder );
        nodeService = builder.nodeService;
    }

    public static Builder create()
    {
        return new Builder();
    }


    public ValidatorResults execute()
    {
        LOG.info( "Running VersionsExecutor..." );
        reportStart();

        final ValidatorResults.Builder results = ValidatorResults.create();

        final BatchedQueryExecutor executor = BatchedQueryExecutor.create().batchSize( BATCH_SIZE ).nodeService( this.nodeService ).build();

        int execute = 0;
        while ( executor.hasMore() )
        {
            LOG.info( "Checking nodes " + execute + "->" + ( execute + BATCH_SIZE ) + " of " + executor.getTotalHits() );
            reportProgress( executor.getTotalHits(), execute );

            results.add( checkNodes( executor.execute() ) );
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
                final ValidatorResult result = doCheckNode( results, nodeId );

                if ( result != null )
                {
                    results.add( result );
                }

            }
            catch ( Exception e )
            {
                LOG.error( "Cannot check versions node with id: " + nodeId + "", e );
            }
        }
        return results;
    }

    private ValidatorResult doCheckNode( final List<ValidatorResult> results, final NodeId nodeId )
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
                results.add( ValidatorResultImpl.create()
                                 .nodeId( nodeId )
                                 .nodePath( nodeVersionsMetadata.getNodePath() )
                                 .nodeVersionId( nodeVersionsMetadata.getNodeVersionId() )
                                 .timestamp( nodeVersionsMetadata.getTimestamp() )
                                 .type( "Unloadable Version" )
                                 .validatorName( validatorName )
                                 .message( "Cannot load version data" )
                                 .repairResult( RepairResult.create()
                                                    .message( "Non repairable automatically" )
                                                    .repairStatus( RepairStatus.NOT_REPAIRABLE )
                                                    .build() )
                                 .build() );
            }
        }

        return null;
    }

    public static final class Builder
        extends AbstractNodeExecutor.Builder<Builder>
    {
        private NodeService nodeService;

        private Builder()
        {
        }

        public Builder nodeService( final NodeService val )
        {
            nodeService = val;
            return this;
        }

        public VersionsExecutor build()
        {
            return new VersionsExecutor( this );
        }
    }
}
