package me.myklebust.xpdoctor.validator;

public class AnalysisCancelledException
    extends RuntimeException
{
    public AnalysisCancelledException()
    {
        super( "Analysis cancelled" );
    }
}
