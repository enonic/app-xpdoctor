package me.myklebust.xpdoctor.validator.result;

import me.myklebust.xpdoctor.validator.RepairStatus;

public interface RepairResult
{
    String message();

    RepairStatus status();
}
