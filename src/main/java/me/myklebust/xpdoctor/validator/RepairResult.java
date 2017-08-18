package me.myklebust.xpdoctor.validator;

public interface RepairResult
{
    String message();

    RepairStatus status();
}
