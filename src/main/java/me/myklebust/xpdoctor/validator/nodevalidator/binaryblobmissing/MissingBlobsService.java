package me.myklebust.xpdoctor.validator.nodevalidator.binaryblobmissing;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.get.GetResponse;

import me.myklebust.xpdoctor.validator.StorageSpyService;

import com.enonic.xp.blob.BlobKey;
import com.enonic.xp.blob.BlobRecord;
import com.enonic.xp.blob.BlobStore;
import com.enonic.xp.blob.SegmentLevel;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeVersionId;
import com.enonic.xp.repository.RepositorySegmentUtils;

import static me.myklebust.xpdoctor.validator.nodevalidator.binaryblobmissing.BlobMissingExecutor.ACCESS_CONTROL_SEGMENT_LEVEL;
import static me.myklebust.xpdoctor.validator.nodevalidator.binaryblobmissing.BlobMissingExecutor.BINARY_SEGMENT_LEVEL;

public class MissingBlobsService
{
    private final BlobStore blobStore;

    private final StorageSpyService storageSpyService;

    public MissingBlobsService( final BlobStore blobStore, final StorageSpyService storageSpyService )
    {
        this.blobStore = blobStore;
        this.storageSpyService = storageSpyService;
    }

    public MissingBlobsResult checkMissingBlobs( final NodeId nodeId )
    {
        MissingBlobsResult missingBlobsResult = new MissingBlobsResult();

        final GetResponse response =
            storageSpyService.getInBranch( nodeId, ContextAccessor.current().getRepositoryId(), ContextAccessor.current().getBranch() );
        final Map<String, Object> sourceAsMap = response.getSourceAsMap();

        final NodeVersionId versionid = NodeVersionId.from( ( (List<String>) sourceAsMap.get( "versionid" ) ).get( 0 ) );

        missingBlobsResult.nodeblobkey = checkBlob( BINARY_SEGMENT_LEVEL, sourceAsMap, "nodeblobkey" );
        missingBlobsResult.accesscontrolblobkey = checkBlob( ACCESS_CONTROL_SEGMENT_LEVEL, sourceAsMap, "accesscontrolblobkey" );
        missingBlobsResult.indexconfigblobkey = checkBlob( BINARY_SEGMENT_LEVEL, sourceAsMap, "indexconfigblobkey" );

        final GetResponse versionResponse = storageSpyService.getVersion( nodeId, versionid, ContextAccessor.current().getRepositoryId() );
        final List<String> binaryblobkeys = (List<String>) versionResponse.getSourceAsMap().get( "binaryblobkeys" );
        if ( binaryblobkeys != null )
        {
            for ( String binaryblobkey : binaryblobkeys )
            {
                final BlobKey blobKey = BlobKey.from( binaryblobkey );
                final BlobRecord binaryBlobRecord = blobStore.getRecord(
                    RepositorySegmentUtils.toSegment( ContextAccessor.current().getRepositoryId(), BINARY_SEGMENT_LEVEL ), blobKey );
                if ( binaryBlobRecord == null )
                {
                    missingBlobsResult.binaryblobkeys.add( blobKey );
                }
            }
        }
        return missingBlobsResult;
    }

    private BlobKey checkBlob( final SegmentLevel segmentLevel, final Map<String, Object> sourceAsMap, final String blobField )
    {
        final BlobKey blobKey = BlobKey.from( ( (List<String>) sourceAsMap.get( blobField ) ).get( 0 ) );
        final BlobRecord blobStoreRecord =
            blobStore.getRecord( RepositorySegmentUtils.toSegment( ContextAccessor.current().getRepositoryId(), segmentLevel ), blobKey );

        if ( blobStoreRecord == null )
        {
            return blobKey;
        }
        else
        {
            return null;
        }
    }

    public static class MissingBlobsResult
    {
        BlobKey accesscontrolblobkey;

        BlobKey indexconfigblobkey;

        BlobKey nodeblobkey;

        Set<BlobKey> binaryblobkeys = new LinkedHashSet<>();

        boolean isOk()
        {
            return accesscontrolblobkey == null && indexconfigblobkey == null && nodeblobkey == null && binaryblobkeys.isEmpty();
        }
    }
}
