package me.myklebust.xpdoctor.validator.nodevalidator.uniquepath;

import java.util.Set;

import com.google.common.collect.Sets;

import com.enonic.xp.node.NodePath;

public class NonUniquePathsHolder
{
    private final Set<NodePath> nonUniquePaths = Sets.newHashSet();

    public void add( final NodePath nodePath )
    {
        this.nonUniquePaths.add( nodePath );
    }

    public boolean has( final NodePath nodePath )
    {
        return this.nonUniquePaths.contains( nodePath );
    }

    public boolean myChildHasAProblem( final NodePath parent )
    {
        for ( final NodePath nodePath : this.nonUniquePaths )
        {
            NodePath currentPath = nodePath;

            while ( currentPath.elementCount() > parent.elementCount() )
            {
                if ( currentPath.getParentPath().equals( parent ) )
                {
                    return true;
                }

                currentPath = currentPath.getParentPath();
            }
        }

        return false;
    }

}
