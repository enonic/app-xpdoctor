package me.myklebust.xpdoctor.validator;

public class RepairResult
{
    private final String message;

    private final RepairStatus repairStatus;

    private RepairResult( final Builder builder )
    {
        message = builder.message;
        repairStatus = builder.repairStatus;
    }

    public String message()
    {
        return this.message;
    }

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

        public RepairResult build()
        {
            return new RepairResult( this );
        }
    }
}
