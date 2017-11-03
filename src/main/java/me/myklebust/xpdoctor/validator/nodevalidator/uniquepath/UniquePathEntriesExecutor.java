package me.myklebust.xpdoctor.validator.nodevalidator.uniquepath;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import me.myklebust.xpdoctor.validator.nodevalidator.AbstractEntriesValidator;
import me.myklebust.xpdoctor.validator.nodevalidator.BatchedQueryExecutor;
import me.myklebust.xpdoctor.validator.result.ValidatorResult;
import me.myklebust.xpdoctor.validator.result.ValidatorResults;

import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeIds;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.repository.RepositoryService;

public class UniquePathEntriesExecutor
    extends AbstractEntriesValidator
{
    public static final int BATCH_SIZE = 1_000;

    private final NodeService nodeService;

    private final RepositoryService repositoryService;

    private final Set<NodePath> handledPaths = Sets.newHashSet();

    private final Logger LOG = LoggerFactory.getLogger( UniquePathEntriesExecutor.class );

    private final UniquePathEntryExecutor entryValidator;

    private UniquePathEntriesExecutor( final Builder builder )
    {
        super( builder );
        nodeService = builder.nodeService;
        repositoryService = builder.repositoryService;
        this.entryValidator = UniquePathEntryExecutor.create().
            validatorName( this.validatorName ).
            nodeService( this.nodeService ).
            repositoryService( this.repositoryService ).
            build();
    }

    public static Builder create()
    {
        return new Builder();
    }

    public ValidatorResults execute()
    {
        LOG.info( "Running UniquePathValidatorExecutor..." );
        reportStart();

        final BatchedQueryExecutor executor = BatchedQueryExecutor.create().
            batchSize( BATCH_SIZE ).
            nodeService( this.nodeService ).
            build();

        final ValidatorResults.Builder results = ValidatorResults.create();

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
                final Node node = this.nodeService.getById( nodeId );

                if ( !handledPaths.contains( node.path() ) )
                {
                    final ValidatorResult result = this.entryValidator.execute( node );

                    if ( result != null )
                    {
                        results.add( result );
                    }

                    this.handledPaths.add( node.path() );
                }
            }
            catch ( Exception e )
            {
                LOG.error( "Cannot check unique path for node with id: " + nodeId + "", e );
            }
        }
        return results;
    }


    public static final class Builder
        extends AbstractEntriesValidator.Builder<Builder>
    {
        private NodeService nodeService;

        private RepositoryService repositoryService;

        private Builder()
        {
        }

        public Builder nodeService( final NodeService val )
        {
            nodeService = val;
            return this;
        }

        public Builder repositoryService( final RepositoryService val )
        {
            repositoryService = val;
            return this;
        }

        public UniquePathEntriesExecutor build()
        {
            return new UniquePathEntriesExecutor( this );
        }
    }
}
