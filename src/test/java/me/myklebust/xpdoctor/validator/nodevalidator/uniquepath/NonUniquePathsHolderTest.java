package me.myklebust.xpdoctor.validator.nodevalidator.uniquepath;

import org.junit.Test;

import com.enonic.xp.node.NodePath;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

public class NonUniquePathsHolderTest
{

    @Test
    public void first_level_child()
        throws Exception
    {
        NodePath parent = NodePath.create( "/a/b/c/d" ).build();
        NodePath child = NodePath.create( "/a/b/c/d/e" ).build();
        final NonUniquePathsHolder holder = new NonUniquePathsHolder();
        holder.add( child );
        assertTrue( holder.myChildHasAProblem( parent ) );
    }

    @Test
    public void second_level_child()
        throws Exception
    {
        NodePath parent = NodePath.create( "/a/b/c/d" ).build();
        NodePath child = NodePath.create( "/a/b/c/d/e/f" ).build();
        final NonUniquePathsHolder holder = new NonUniquePathsHolder();
        holder.add( child );
        assertTrue( holder.myChildHasAProblem( parent ) );
    }

    @Test
    public void same()
        throws Exception
    {
        NodePath parent = NodePath.create( "/a/b/c/d" ).build();
        NodePath child = NodePath.create( "/a/b/c/d" ).build();
        final NonUniquePathsHolder holder = new NonUniquePathsHolder();
        holder.add( child );
        assertFalse( holder.myChildHasAProblem( parent ) );
    }

    @Test
    public void less()
        throws Exception
    {
        NodePath parent = NodePath.create( "/a/b/c/d" ).build();
        NodePath child = NodePath.create( "/a/b/c" ).build();
        final NonUniquePathsHolder holder = new NonUniquePathsHolder();
        holder.add( child );
        assertFalse( holder.myChildHasAProblem( parent ) );
    }


    @Test
    public void not_parent()
        throws Exception
    {
        NodePath parent = NodePath.create( "/a/b/c/d" ).build();
        NodePath child = NodePath.create( "/b/b/c/d/e/f" ).build();
        final NonUniquePathsHolder holder = new NonUniquePathsHolder();
        holder.add( child );
        assertFalse( holder.myChildHasAProblem( parent ) );
    }
}