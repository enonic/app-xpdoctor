package me.myklebust.xpdoctor.validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.branch.Branches;
import com.enonic.xp.repository.RepositoryId;

public class ValidateParams
{
    private List<String> enabledValidators;

    private Map<RepositoryId, Branches> repoBranches = new HashMap<>();

    public Map<RepositoryId, Branches> getRepoBranches()
    {
        return repoBranches;
    }

    public void addRepoBranch( final String repoId, final String branch )
    {
        final RepositoryId repositoryId = RepositoryId.from( repoId );

        final Branches branches = this.repoBranches.get( repositoryId );

        if ( branches != null )
        {
            this.repoBranches.put( repositoryId, Branches.from(
                Stream.concat( branches.stream(), Stream.of( Branch.from( branch ) ) ).toArray( Branch[]::new ) ) );
        }
        else
        {
            this.repoBranches.put( repositoryId, Branches.from( Branch.from( branch ) ) );
        }
    }

    public void setEnabledValidators( final List<String> enabledValidators )
    {
        this.enabledValidators = enabledValidators;
    }

    public List<String> getEnabledValidators()
    {
        return enabledValidators;
    }

    public void setEnabledValidators( final String[] enabledValidators )
    {

        this.enabledValidators = Arrays.asList( enabledValidators );
    }

    public void setEnabledValidators( final String enabledValidator )
    {

        this.enabledValidators = new ArrayList<>( List.of(enabledValidator) );
    }

}
