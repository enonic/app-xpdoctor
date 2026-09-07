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
        NodePath parent = new NodePath( "/a/b/c/d" );
        NodePath child = new NodePath( "/a/b/c/d/e" );
        final NonUniquePathsHolder holder = new NonUniquePathsHolder();
        holder.add( child );
        assertTrue( holder.myChildHasAProblem( parent ) );
    }

    @Test
    public void second_level_child()
        throws Exception
    {
        NodePath parent = new NodePath( "/a/b/c/d" );
        NodePath child = new NodePath( "/a/b/c/d/e/f" );
        final NonUniquePathsHolder holder = new NonUniquePathsHolder();
        holder.add( child );
        assertTrue( holder.myChildHasAProblem( parent ) );
    }

    @Test
    public void same()
        throws Exception
    {
        NodePath parent = new NodePath( "/a/b/c/d" );
        NodePath child = new NodePath( "/a/b/c/d" );
        final NonUniquePathsHolder holder = new NonUniquePathsHolder();
        holder.add( child );
        assertFalse( holder.myChildHasAProblem( parent ) );
    }

    @Test
    public void less()
        throws Exception
    {
        NodePath parent = new NodePath( "/a/b/c/d" );
        NodePath child = new NodePath( "/a/b/c" );
        final NonUniquePathsHolder holder = new NonUniquePathsHolder();
        holder.add( child );
        assertFalse( holder.myChildHasAProblem( parent ) );
    }


    @Test
    public void not_parent()
        throws Exception
    {
        NodePath parent = new NodePath( "/a/b/c/d" );
        NodePath child = new NodePath( "/b/b/c/d/e/f" );
        final NonUniquePathsHolder holder = new NonUniquePathsHolder();
        holder.add( child );
        assertFalse( holder.myChildHasAProblem( parent ) );
    }
}