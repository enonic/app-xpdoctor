package me.myklebust.xpdoctor.validator;

public class RepairResultImpl
    implements RepairResult
{

    private final String message;

    private final RepairStatus repairStatus;

    private RepairResultImpl( final Builder builder )
    {
        message = builder.message;
        repairStatus = builder.repairStatus;
    }

    @Override
    public String message()
    {
        return this.message;
    }

    @Override
    public RepairStatus status()
    {
        return this.repairStatus;
    }

    public static Builder create()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private String message;

        private RepairStatus repairStatus;

        private Builder()
        {
        }

        public Builder message( final String val )
        {
            message = val;
            return this;
        }

        public Builder repairStatus( final RepairStatus val )
        {
            repairStatus = val;
            return this;
        }

        public RepairResultImpl build()
        {
            return new RepairResultImpl( this );
        }
    }
}
