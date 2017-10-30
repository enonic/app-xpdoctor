package me.myklebust.xpdoctor.validator;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

public class ValidateParams
{
    private List<String> enabledValidators;

    public void setEnabledValidators( final List<String> enabledValidators )
    {
        this.enabledValidators = enabledValidators;
    }

    public List<String> getEnabledValidators()
    {
        return enabledValidators;
    }

    public void setEnabledValidators( final String[] enabledValidators )
    {
        this.enabledValidators = Arrays.asList( enabledValidators );
    }

    public void setEnabledValidators( final String enabledValidator )
    {
        this.enabledValidators = Lists.newArrayList( enabledValidator );
    }

}
