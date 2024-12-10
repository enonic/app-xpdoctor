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
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeVersionId;
import com.enonic.xp.repository.RepositorySegmentUtils;

import static me.myklebust.xpdoctor.validator.nodevalidator.binaryblobmissing.BinaryBlobMissingExecutor.ACCESS_CONTROL_SEGMENT_LEVEL;
import static me.myklebust.xpdoctor.validator.nodevalidator.binaryblobmissing.BinaryBlobMissingExecutor.BINARY_SEGMENT_LEVEL;

public class MissingBlobsService
{
    private final BlobStore blobStore;

    private final StorageSpyService storageSpyService;

    public MissingBlobsService( final BlobStore blobStore, final StorageSpyService storageSpyService )
    {
        this.blobStore = blobStore;
        this.storageSpyService = storageSpyService;
    }

    public BlobRefs missingBlobs( final NodeId nodeId )
    {
        BlobRefs blobRefs = new BlobRefs();

        final GetResponse response =
            storageSpyService.getInBranch( nodeId, ContextAccessor.current().getRepositoryId(), ContextAccessor.current().getBranch() );
        final Map<String, Object> sourceAsMap = response.getSourceAsMap();
        final NodeVersionId versionid = NodeVersionId.from( ( (List<String>) sourceAsMap.get( "versionid" ) ).get( 0 ) );

        final BlobKey accesscontrolblobkey = BlobKey.from( ( (List<String>) sourceAsMap.get( "accesscontrolblobkey" ) ).get( 0 ) );

        final BlobRecord accesscontrolblobRecord = blobStore.getRecord(
            RepositorySegmentUtils.toSegment( ContextAccessor.current().getRepositoryId(), ACCESS_CONTROL_SEGMENT_LEVEL ),
            accesscontrolblobkey );
        if ( accesscontrolblobRecord == null )
        {
            blobRefs.accesscontrolblobkey = accesscontrolblobkey;
        }
        final BlobKey indexconfigblobkey = BlobKey.from( ( (List<String>) sourceAsMap.get( "indexconfigblobkey" ) ).get( 0 ) );
        final BlobRecord indexconfigblobRecord =
            blobStore.getRecord( RepositorySegmentUtils.toSegment( ContextAccessor.current().getRepositoryId(), BINARY_SEGMENT_LEVEL ),
                                 indexconfigblobkey );
        if ( indexconfigblobRecord == null )
        {
            blobRefs.indexconfigblobkey = indexconfigblobkey;
        }

        final BlobKey nodeblobkey = BlobKey.from( ( (List<String>) sourceAsMap.get( "nodeblobkey" ) ).get( 0 ) );
        final BlobRecord nodeblobRecord =
            blobStore.getRecord( RepositorySegmentUtils.toSegment( ContextAccessor.current().getRepositoryId(), BINARY_SEGMENT_LEVEL ),
                                 nodeblobkey );
        if ( nodeblobRecord == null )
        {
            blobRefs.nodeblobkey = nodeblobkey;
        }

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
                    blobRefs.binaryblobkeys.add( blobKey );
                }
            }
        }
        return blobRefs;
    }

    public static class BlobRefs
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
