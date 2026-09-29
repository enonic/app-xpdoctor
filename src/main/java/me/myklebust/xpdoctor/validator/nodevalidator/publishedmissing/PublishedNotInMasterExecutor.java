package me.myklebust.xpdoctor.validator.nodevalidator.publishedmissing;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.myklebust.xpdoctor.validator.StorageSpyService;
import me.myklebust.xpdoctor.validator.ValidatorResult;
import me.myklebust.xpdoctor.validator.nodevalidator.Reporter;
import me.myklebust.xpdoctor.validator.nodevalidator.ScrollQueryExecutor;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.branch.Branches;
import com.enonic.xp.content.ContentConstants;
import com.enonic.xp.content.ContentPropertyNames;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.data.PropertySet;
import com.enonic.xp.node.GetActiveNodeVersionsParams;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeIds;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.node.NodeVersion;

public class PublishedNotInMasterExecutor
{
    private static final Logger LOG = LoggerFactory.getLogger( PublishedNotInMasterExecutor.class );

    private final NodeService nodeService;

    private final StorageSpyService storageSpyService;

    private final PublishedNotInMasterDoctor doctor;

    public PublishedNotInMasterExecutor( final NodeService nodeService, final StorageSpyService storageSpyService,
                                         final PublishedNotInMasterDoctor doctor )
    {
        this.nodeService = nodeService;
        this.storageSpyService = storageSpyService;
        this.doctor = doctor;
    }

    public void execute( final Reporter reporter )
    {
        // The publish state lives in draft: the master scan has nothing to check
        if ( !isContentDraft() )
        {
            return;
        }

        LOG.info( "Running PublishedNotInMasterExecutor..." );
        reporter.reportStart();

        ScrollQueryExecutor.create()
            .progressReporter( reporter.getProgressReporter() )
            .indexType( ScrollQueryExecutor.IndexType.STORAGE )
            .spyStorageService( this.storageSpyService )
            .build()
            .execute( nodesToCheck -> checkNodes( nodesToCheck, reporter ) );

        LOG.info( "... PublishedNotInMasterExecutor done" );
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
                LOG.error( "Cannot check publish state of node with id: {}", nodeId, e );
            }
        }
    }

    private void doCheckNode( final Reporter results, final NodeId nodeId )
    {
        final Node node = this.nodeService.getById( nodeId );
        if ( !ContentConstants.CONTENT_NODE_COLLECTION.equals( node.getNodeType() ) )
        {
            return;
        }

        // Publishing sets publish.from, unpublishing removes it
        final PropertySet publishInfo = node.data().getSet( ContentPropertyNames.PUBLISH_INFO );
        if ( publishInfo == null || publishInfo.getInstant( ContentPropertyNames.PUBLISH_FROM ) == null )
        {
            return;
        }

        final Map<Branch, NodeVersion> versions = this.nodeService.getActiveVersions( GetActiveNodeVersionsParams.create()
                                                                                          .nodeId( nodeId )
                                                                                          .branches( Branches.from(
                                                                                              ContentConstants.BRANCH_MASTER ) )
                                                                                          .build() ).getNodeVersions();
        if ( versions.get( ContentConstants.BRANCH_MASTER ) != null )
        {
            return;
        }

        results.addResult( ValidatorResult.create()
                               .nodeId( nodeId )
                               .nodePath( node.path() )
                               .nodeVersionId( node.getNodeVersionId() )
                               .timestamp( node.getTimestamp() )
                               .type( "Published, not in master" )
                               .validatorName( results.validatorName )
                               .message( "Content is published according to draft, but does not exist in master" )
                               .repairResult( this.doctor.repairNode( nodeId, true ) ) );
    }

    private static boolean isContentDraft()
    {
        return ContentConstants.BRANCH_DRAFT.equals( ContextAccessor.current().getBranch() ) &&
            ContextAccessor.current().getRepositoryId().toString().startsWith( ContentConstants.CONTENT_REPO_ID_PREFIX );
    }
}
