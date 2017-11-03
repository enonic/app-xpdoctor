package me.myklebust.xpdoctor.validator.model;

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;

public class IssueEntries
    implements Iterable<IssueEntry>
{
    private List<IssueEntry> issueEntries = Lists.newArrayList();

    @Override
    public Iterator<IssueEntry> iterator()
    {
        return this.issueEntries.iterator();
    }

    public void setIssueEntries( final List<IssueEntry> issueEntries )
    {
        this.issueEntries = issueEntries;
    }

    public List<IssueEntry> getIssueEntries()
    {
        return issueEntries;
    }

    public void add( final IssueEntry issueEntry ) {
        this.issueEntries.add( issueEntry );
    }
}