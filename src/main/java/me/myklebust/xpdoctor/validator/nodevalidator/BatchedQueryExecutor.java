package me.myklebust.xpdoctor.validator.nodevalidator;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.elasticsearch.action.search.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.myklebust.xpdoctor.validator.StorageSpyService;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeIds;
import com.enonic.xp.repository.RepositoryId;
import com.enonic.xp.task.ProgressReporter;

public class BatchedQueryExecutor
{
    private static final Logger LOG = LoggerFactory.getLogger( BatchedQueryExecutor.class );

    private final int batchSize;

    private final StorageSpyService storageSpyService;

    private final ProgressReporter progressReporter;

    private final RepositoryId repositoryId;

    private final Branch branch;

    private BatchedQueryExecutor( final Builder builder )
    {
        this.batchSize = 1_000;
        this.storageSpyService = builder.storageSpyService;
        this.progressReporter = builder.progressReporter;
        this.repositoryId = ContextAccessor.current().getRepositoryId();
        this.branch = ContextAccessor.current().getBranch();
    }

    public void execute( Consumer<NodeIds> consumer )
    {
        int totalHits = (int) this.storageSpyService.findAllInBranch( repositoryId, branch, 0, null ).getHits().getTotalHits();

        NodeId lastNodeId = null;

        int currentFrom = 0;
        while ( true )
        {
            final NodeIds nodeIds = executeNext( lastNodeId );
            if ( nodeIds.isEmpty() )
            {
                return;
            }
            else
            {
                LOG.info( "Checking nodes {}-{} of {}", currentFrom, Math.min( currentFrom - 1 + batchSize, totalHits ) , totalHits );
                progressReporter.progress( totalHits, currentFrom );
                currentFrom += nodeIds.getSize();

                consumer.accept( nodeIds );
                lastNodeId = nodeIds.stream().sequential().reduce((first, second) -> second).orElse(null);
            }
        }
    }

    public NodeIds executeNext( final NodeId lastNodeId )
    {
        final SearchResponse allInBranch =
            this.storageSpyService.findAllInBranch( repositoryId, branch, batchSize, lastNodeId == null ? null : lastNodeId.toString() );
        return NodeIds.from( Arrays.stream( allInBranch.getHits().getHits() )
                                 .map( h -> ((List<String>)h.getSource().get( "nodeid" )).get( 0 ) )
                                 .map( NodeId::from )
                                 .toArray( NodeId[]::new ) );

    }

    public static Builder create()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private StorageSpyService storageSpyService;

        private ProgressReporter progressReporter;

        private Builder()
        {
        }

        public Builder spyStorageService( final StorageSpyService val )
        {
            storageSpyService = val;
            return this;
        }

        public Builder progressReporter( final ProgressReporter val )
        {
            progressReporter = val;
            return this;
        }

        public BatchedQueryExecutor build()
        {
            return new BatchedQueryExecutor( this );
        }
    }
}
