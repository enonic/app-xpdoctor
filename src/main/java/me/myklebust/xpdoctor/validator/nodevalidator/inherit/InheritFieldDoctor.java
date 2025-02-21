package me.myklebust.xpdoctor.validator.nodevalidator.inherit;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.elasticsearch.action.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.ByteSource;

import me.myklebust.xpdoctor.json.ObjectMapperHelper;
import me.myklebust.xpdoctor.validator.RepairResult;
import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.nodevalidator.NodeDoctor;
import me.myklebust.xpdoctor.validator.nodevalidator.uniquepath.UniquePathDoctor;

import com.enonic.xp.blob.BlobRecord;
import com.enonic.xp.blob.BlobStore;
import com.enonic.xp.blob.Segment;
import com.enonic.xp.blob.SegmentLevel;
import com.enonic.xp.branch.Branches;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.node.GetActiveNodeVersionsParams;
import com.enonic.xp.node.GetActiveNodeVersionsResult;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.node.NodeVersionMetadata;
import com.enonic.xp.repository.RepositorySegmentUtils;

public class InheritFieldDoctor
    implements NodeDoctor
{
    private static final Logger LOG = LoggerFactory.getLogger( UniquePathDoctor.class );

    private final NodeService nodeService;

    private IndexValueService indexValueService;

    private BlobStore blobStore;

    private static final ObjectMapper MAPPER = ObjectMapperHelper.create();

    public InheritFieldDoctor( final Builder builder )
    {
        this.nodeService = builder.nodeService;
        this.indexValueService = builder.indexValueService;
        this.blobStore = builder.blobStore;
    }

    @Override
    public RepairResult repairNode( final NodeId nodeId, final boolean dryRun )
    {
        LOG.info( "Updating blob with index value" );

        try
        {
            final Set<String> indexValue = indexValueService.getFieldsValue( nodeId, "search", InheritFieldValidator.FIELDS_TO_VALIDATE );

            final GetActiveNodeVersionsResult versionsResult = nodeService.getActiveVersions( GetActiveNodeVersionsParams.create()
                                                                                                  .nodeId( nodeId )
                                                                                                  .branches( Branches.from(
                                                                                                      ContextAccessor.current()
                                                                                                          .getBranch() ) )
                                                                                                  .build() );

            final NodeVersionMetadata nodeVersionMetadata = versionsResult.getNodeVersions().get( ContextAccessor.current().getBranch() );
            if ( nodeVersionMetadata != null )
            {
                final Segment nodeSegment =
                    RepositorySegmentUtils.toSegment( ContextAccessor.current().getRepositoryId(), SegmentLevel.from( "node" ) );
                final BlobRecord nodeBlobRecord =
                    blobStore.getRecord( nodeSegment, nodeVersionMetadata.getNodeVersionKey().getNodeBlobKey() );

                final JsonNode nodeVersionJson = MAPPER.readTree( nodeBlobRecord.getBytes().read() );

                final Iterator<Map.Entry<String, JsonNode>> fields = nodeVersionJson.fields();
                while ( fields.hasNext() )
                {

                    final Map.Entry<String, JsonNode> entry = fields.next();

                    if ( "data".equals( entry.getKey() ) )
                    {
                        final JsonNode dataNode = entry.getValue();

                        final Iterator<JsonNode> dataFields = dataNode.elements();

                        while ( dataFields.hasNext() )
                        {
                            final JsonNode dataField = dataFields.next();
                            final String dataFieldName =
                                Optional.ofNullable( dataField.findValue( "name" ) ).map( JsonNode::textValue ).orElse( null );

                            if ( "inherit".equals( dataFieldName ) )
                            {
                                final ArrayNode valuesArray = ( (ObjectNode) dataField ).putArray( "values" );

                                indexValue.forEach( te -> valuesArray.add( MAPPER.createObjectNode().put( "v", te ) ) );

                                final byte[] modifiedNode = MAPPER.writeValueAsBytes( nodeVersionJson );

                                final BlobRecord modifiedBlobRecord = blobStore.addRecord( nodeSegment, ByteSource.wrap( modifiedNode ) );

                                indexValueService.update(
                                    new UpdateRequest( "storage-" + ContextAccessor.current().getRepositoryId(), "version",
                                                       nodeVersionMetadata.getNodeVersionId().toString() ).doc(
                                        Map.of( "nodeblobkey", new String[]{modifiedBlobRecord.getKey().toString()} ) ).routing(nodeId.toString()) );

                                indexValueService.update(
                                    new UpdateRequest( "storage-" + ContextAccessor.current().getRepositoryId(), "branch",
                                                       nodeId + "_" + ContextAccessor.current().getBranch() ).doc(
                                        Map.of( "nodeblobkey", new String[]{modifiedBlobRecord.getKey().toString()} ) ).routing(nodeId.toString()) );

                                final String msg = "Created new blob with key: [" + modifiedBlobRecord.getKey() + "], current node version and branch are updated with it";

                                LOG.info( msg );

                                return RepairResult.create().message( msg ).repairStatus( RepairStatus.REPAIRED ).build();

                            }

                        }

                    }
                }
            }
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to repair node", e );

            return RepairResult.create()
                .message( "Cannot repair node, exception when trying to load: " + e.getMessage() )
                .repairStatus( RepairStatus.FAILED )
                .build();
        }

        return RepairResult.create().
            message( "No need to repair, the node has no 'inherit' value" ).
            repairStatus( RepairStatus.UNKNOW ).
            build();
    }

    public static Builder create()
    {
        return new Builder();
    }


    public static final class Builder
    {
        private NodeService nodeService;

        private IndexValueService indexValueService;

        private BlobStore blobStore;

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

        public Builder blobStore( final BlobStore val )
        {
            blobStore = val;
            return this;
        }

        public InheritFieldDoctor build()
        {
            return new InheritFieldDoctor( this );
        }
    }

}
