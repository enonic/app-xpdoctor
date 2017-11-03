package me.myklebust.xpdoctor.validator;

import me.myklebust.xpdoctor.validator.result.RepairResult;
import me.myklebust.xpdoctor.validator.result.ValidatorResult;
import me.myklebust.xpdoctor.validator.result.ValidatorResults;

import com.enonic.xp.node.NodeId;
import com.enonic.xp.task.ProgressReporter;

public interface Validator
    extends Comparable<Validator>
{
    String name();

    String getDescription();

    String getRepairStrategy();

    ValidatorResults validate( final ProgressReporter reporter );

    ValidatorResult validate( final NodeId nodeId );

    RepairResult repair( final NodeId nodeId );

    int order();


}
