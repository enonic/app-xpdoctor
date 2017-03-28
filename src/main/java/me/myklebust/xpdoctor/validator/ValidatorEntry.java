package me.myklebust.xpdoctor.validator;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodePath;

public class ValidatorEntry
{
    private NodePath nodePath;

    private NodeId nodeId;

    public ValidatorEntry( final NodePath nodePath, final NodeId nodeId )
    {
        this.nodePath = nodePath;
        this.nodeId = nodeId;
    }

    public static ValidatorEntry create( final NodePath nodePath, final NodeId nodeId )
    {
        return new ValidatorEntry( nodePath, nodeId );
    }

    public NodePath getNodePath()
    {
        return nodePath;
    }

    public NodeId getNodeId()
    {
        return nodeId;
    }

    @Override
    public boolean equals( final Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        final ValidatorEntry validatorEntry = (ValidatorEntry) o;

        if ( nodePath != null ? !nodePath.equals( validatorEntry.nodePath ) : validatorEntry.nodePath != null )
        {
            return false;
        }
        return nodeId != null ? nodeId.equals( validatorEntry.nodeId ) : validatorEntry.nodeId == null;

    }

    @Override
    public int hashCode()
    {
        int result = nodePath != null ? nodePath.hashCode() : 0;
        result = 31 * result + ( nodeId != null ? nodeId.hashCode() : 0 );
        return result;
    }
}



