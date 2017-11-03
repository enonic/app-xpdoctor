package me.myklebust.xpdoctor.validator.nodevalidator.parentexists;

import me.myklebust.xpdoctor.validator.RepairStatus;
import me.myklebust.xpdoctor.validator.result.RepairResultImpl;
import me.myklebust.xpdoctor.validator.result.ValidatorResult;
import me.myklebust.xpdoctor.validator.result.ValidatorResultImpl;

import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.node.NodeService;

public class ParentExistsEntryExecutor
{
    private final NodeService nodeService;

    private final String validatorName;

    private ParentExistsEntryExecutor( final Builder builder )
    {
        nodeService = builder.nodeService;
        validatorName = builder.validatorName;
    }

    public static Builder create()
    {
        return new Builder();
    }

    public ValidatorResult execute( final NodeId nodeId )
    {
        final Node node = this.nodeService.getById( nodeId );

        final NodePath parentPath = node.path().getParentPath();

        final Node parent = this.nodeService.getByPath( parentPath );

        if ( parent == null )
        {
            return ValidatorResultImpl.create().
                nodeId( nodeId ).
                nodePath( node.path() ).
                nodeVersionId( node.getNodeVersionId() ).
                timestamp( node.getTimestamp() ).
                type( "No parent" ).
                validatorName( validatorName ).
                message( "Parent with path : " + node.parentPath() + " not found" ).
                repairResult( RepairResultImpl.create().
                    message( "Move to folder: XX" ).
                    repairStatus( RepairStatus.IS_REPAIRABLE ).
                    build() ).
                build();
        }

        return null;
    }

    public static final class Builder
    {
        private NodeService nodeService;

        private String validatorName;

        private Builder()
        {
        }

        public Builder nodeService( final NodeService val )
        {
            nodeService = val;
            return this;
        }

        public Builder validatorName( final String val )
        {
            validatorName = val;
            return this;
        }

        public ParentExistsEntryExecutor build()
        {
            return new ParentExistsEntryExecutor( this );
        }
    }
}
