package me.myklebust.xpdoctor.validator;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.task.ProgressReporter;

public interface Validator
    extends Comparable<Validator>
{
    String name();

    String getDescription();

    String getRepairStrategy();

    ValidatorResults validate( final ProgressReporter reporter );

    RepairResult repair( final NodeId nodeId );

    int order();


}
