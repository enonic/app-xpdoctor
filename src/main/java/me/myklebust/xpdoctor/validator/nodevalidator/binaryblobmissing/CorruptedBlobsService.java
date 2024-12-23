package me.myklebust.xpdoctor.validator.nodevalidator.binaryblobmissing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

import static me.myklebust.xpdoctor.validator.nodevalidator.binaryblobmissing.BlobCorruptedDoctor.ACCESS_CONTROL_SEGMENT_LEVEL;
import static me.myklebust.xpdoctor.validator.nodevalidator.binaryblobmissing.BlobCorruptedDoctor.BINARY_SEGMENT_LEVEL;
import static me.myklebust.xpdoctor.validator.nodevalidator.binaryblobmissing.BlobCorruptedDoctor.INDEX_CONFIG_SEGMENT_LEVEL;
import static me.myklebust.xpdoctor.validator.nodevalidator.binaryblobmissing.BlobCorruptedDoctor.NODE_SEGMENT_LEVEL;

public class CorruptedBlobsService
{

    private final BlobStore blobStore;

    private final StorageSpyService storageSpyService;

    public CorruptedBlobsService( final BlobStore blobStore, final StorageSpyService storageSpyService )
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

        checkBlob( NODE_SEGMENT_LEVEL, sourceAsMap, "nodeblobkey", missingBlobsResult );
        checkBlob( ACCESS_CONTROL_SEGMENT_LEVEL, sourceAsMap, "accesscontrolblobkey", missingBlobsResult );
        checkBlob( INDEX_CONFIG_SEGMENT_LEVEL, sourceAsMap, "indexconfigblobkey", missingBlobsResult );

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
                    missingBlobsResult.blobReports.add( new BlobReport( BINARY_SEGMENT_LEVEL, blobKey, BlobReport.BlobState.MISSING ) );
                }
                else
                {
                    if ( !binaryBlobRecord.getKey().equals( BlobKey.from( binaryBlobRecord.getBytes() ) ) )
                    {
                        missingBlobsResult.blobReports.add( new BlobReport( BINARY_SEGMENT_LEVEL, blobKey, BlobReport.BlobState.CORRUPTED ) );
                    }
                }
            }
        }
        return missingBlobsResult;
    }

    private void checkBlob( final SegmentLevel segmentLevel, final Map<String, Object> sourceAsMap, final String blobField, MissingBlobsResult missingBlobsResult )
    {
        final BlobKey blobKey = BlobKey.from( ( (List<String>) sourceAsMap.get( blobField ) ).get( 0 ) );
        final BlobRecord blobStoreRecord =
            blobStore.getRecord( RepositorySegmentUtils.toSegment( ContextAccessor.current().getRepositoryId(), segmentLevel ), blobKey );

        if ( blobStoreRecord == null )
        {
            missingBlobsResult.blobReports.add( new BlobReport( segmentLevel, blobKey, BlobReport.BlobState.MISSING ) );
        }
        else if ( !blobStoreRecord.getKey().equals( BlobKey.from( blobStoreRecord.getBytes() ) ) )
        {
            missingBlobsResult.blobReports.add( new BlobReport( segmentLevel, blobKey, BlobReport.BlobState.CORRUPTED ) );
        }
    }

    public static class MissingBlobsResult
    {
        List<BlobReport> blobReports = new ArrayList<>();
        boolean isOk()
        {
            return blobReports.isEmpty();
        }

        @Override
        public String toString()
        {
            return blobReports.toString();
        }
    }

}
