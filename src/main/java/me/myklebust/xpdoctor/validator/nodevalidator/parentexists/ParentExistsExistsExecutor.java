package me.myklebust.xpdoctor.validator.nodevalidator.parentexists;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import me.myklebust.xpdoctor.validator.ValidatorResult;
import me.myklebust.xpdoctor.validator.ValidatorResults;
import me.myklebust.xpdoctor.validator.nodevalidator.BatchedQueryExecutor;

import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeIds;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.node.NodeService;

public class ParentExistsExistsExecutor
{
    private final NodeService nodeService;

    private final Logger LOG = LoggerFactory.getLogger( ParentExistsExistsExecutor.class );

    public ParentExistsExistsExecutor( final NodeService nodeService )
    {
        this.nodeService = nodeService;
    }

    public ValidatorResults execute()
    {
        final BatchedQueryExecutor executor = BatchedQueryExecutor.create().
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
                doCheckNode( results, nodeId );
            }
            catch ( Exception e )
            {
                LOG.error( "Cannot check parent exists for node with id: " + nodeId + "", e );
            }
        }
        return results;
    }

    private void doCheckNode( final List<ValidatorResult> results, final NodeId nodeId )
    {
        final Node node = this.nodeService.getById( nodeId );

        final NodePath parentPath = node.path().getParentPath();

        final Node parent = this.nodeService.getByPath( parentPath );

        if ( parent == null )
        {
            results.add( ParentNotExistsResult.create().
                nodeId( node.id() ).
                nodePath( node.path() ).
                build() );
        }
    }

}
