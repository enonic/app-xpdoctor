package me.myklebust.xpdoctor.validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RepairAllParams
{
    private List<String> issueIds = new ArrayList<>();

    private boolean all = false;

    public List<String> getIssueIds()
    {
        return issueIds;
    }

    public void setIssueIds( final List<String> issueIds )
    {
        this.issueIds = issueIds;
    }

    public void setIssueIds( final String[] issueIds )
    {
        this.issueIds = Arrays.asList( issueIds );
    }

    public void setIssueIds( final String issueId )
    {
        this.issueIds = new ArrayList<>( List.of( issueId ) );
    }

    public boolean isAll()
    {
        return all;
    }

    public void setAll( final boolean all )
    {
        this.all = all;
    }
}
