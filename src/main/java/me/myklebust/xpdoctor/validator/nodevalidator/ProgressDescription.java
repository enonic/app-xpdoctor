package me.myklebust.xpdoctor.validator.nodevalidator;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.repository.RepositoryId;

public class ProgressDescription
{
    private final String repositoryId;

    private final String branch;

    private final String validator;

    private final int current;

    private final int total;

    private ProgressDescription( final Builder builder )
    {
        repositoryId = builder.repositoryId == null ? "" : builder.repositoryId.toString();
        branch = builder.branch == null ? "" : builder.branch.toString();
        validator = builder.validator;
        current = builder.current;
        total = builder.total;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public String getBranch()
    {
        return branch;
    }

    public String getValidator()
    {
        return validator;
    }

    public int getCurrent()
    {
        return current;
    }

    public int getTotal()
    {
        return total;
    }

    public String toJson()
    {
        return "{\"repositoryId\":\"" + escapeJson( repositoryId ) + "\"" +
            ",\"branch\":\"" + escapeJson( branch ) + "\"" +
            ",\"validator\":\"" + escapeJson( validator ) + "\"" +
            ",\"current\":" + current +
            ",\"total\":" + total + "}";
    }

    private static String escapeJson( final String value )
    {
        if ( value == null )
        {
            return "";
        }
        return value.replace( "\\", "\\\\" ).replace( "\"", "\\\"" ).replace( "\n", "\\n" ).replace( "\r", "\\r" ).replace( "\t", "\\t" );
    }

    public static Builder create()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private RepositoryId repositoryId;

        private Branch branch;

        private String validator;

        private int current;

        private int total;

        private Builder()
        {
        }

        public Builder repositoryId( final RepositoryId val )
        {
            repositoryId = val;
            return this;
        }

        public Builder branch( final Branch val )
        {
            branch = val;
            return this;
        }

        public Builder validator( final String validator )
        {
            this.validator = validator;
            return this;
        }

        public Builder current( final int val )
        {
            current = val;
            return this;
        }

        public Builder total( final int val )
        {
            total = val;
            return this;
        }

        public ProgressDescription build()
        {
            return new ProgressDescription( this );
        }
    }
}
