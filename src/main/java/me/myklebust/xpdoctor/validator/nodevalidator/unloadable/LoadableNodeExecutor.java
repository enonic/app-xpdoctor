package me.myklebust.xpdoctor.validator.nodevalidator.unloadable;

import java.util.List;

import com.google.common.collect.Lists;

import me.myklebust.xpdoctor.validator.ValidatorResult;
import me.myklebust.xpdoctor.validator.ValidatorResults;
import me.myklebust.xpdoctor.validator.nodevalidator.BatchedQueryExecutor;

import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeIds;
import com.enonic.xp.node.NodeService;

public class LoadableNodeExecutor
{
    private final NodeService nodeService;

    public LoadableNodeExecutor( final NodeService nodeService )
    {
        this.nodeService = nodeService;
    }

    public ValidatorResults execute()
    {
        final BatchedQueryExecutor executor = BatchedQueryExecutor.create().
            batchSize( 5_000 ).
            nodeService( this.nodeService ).
            build();

        final ValidatorResults.Builder results = ValidatorResults.create();

        while ( executor.hasMore() )
        {
            results.add( checkNodes( executor.execute() ) );
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
                final Node node = this.nodeService.getById( nodeId );
            }
            catch ( Exception e )
            {
                results.add( UnloadableNodeResult.create().
                    nodeId( nodeId ).
                    exception( e ).
                    build() );
            }
        }
        return results;
    }
}
