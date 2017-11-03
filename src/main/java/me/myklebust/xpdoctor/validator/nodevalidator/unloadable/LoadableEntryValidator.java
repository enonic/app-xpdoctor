package me.myklebust.xpdoctor.validator.nodevalidator.unloadable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.myklebust.xpdoctor.validator.result.RepairResult;
import me.myklebust.xpdoctor.validator.result.ValidatorResult;
import me.myklebust.xpdoctor.validator.result.ValidatorResultImpl;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeService;

public class LoadableEntryValidator
{
    private final Logger LOG = LoggerFactory.getLogger( LoadableEntryValidator.class );

    private final NodeService nodeService;

    private final String validatorName;

    private final LoadableNodeDoctor doctor;

    private LoadableEntryValidator( final Builder builder )
    {
        nodeService = builder.nodeService;
        validatorName = builder.validatorName;
        doctor = builder.doctor;
    }

    public static Builder create()
    {
        return new Builder();
    }

    public ValidatorResult execute( final NodeId nodeId )
    {
        try
        {
            this.nodeService.getById( nodeId );
        }
        catch ( Exception e )
        {
            final RepairResult repairResult;
            try
            {
                repairResult = this.doctor.repaidNode( nodeId, false );

                final ValidatorResultImpl result = ValidatorResultImpl.create().
                    nodeId( nodeId ).
                    nodePath( null ).
                    nodeVersionId( null ).
                    timestamp( null ).
                    type( "Node not loadable" ).
                    validatorName( validatorName ).
                    message( e.getMessage() ).
                    repairResult( repairResult ).
                    build();

                return result;
            }
            catch ( Exception e1 )
            {
                LOG.error( "Failed to repair", e1 );
            }
        }

        return null;
    }

    public static final class Builder
    {
        private NodeService nodeService;

        private String validatorName;

        private LoadableNodeDoctor doctor;

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

        public Builder doctor( final LoadableNodeDoctor val )
        {
            doctor = val;
            return this;
        }

        public LoadableEntryValidator build()
        {
            return new LoadableEntryValidator( this );
        }
    }
}
