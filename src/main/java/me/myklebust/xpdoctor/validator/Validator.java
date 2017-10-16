package me.myklebust.xpdoctor.validator;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.task.ProgressReporter;

public interface Validator
{
    String name();

    String getDescription();

    String getRepairStrategy();

    ValidatorResults validate( final ProgressReporter reporter );

    boolean repair( final NodeId nodeId );

}
