package com.avides.springboot.springtainer.common.container;

public class ContainerStartupFailedException extends Exception
{
    private static final long serialVersionUID = -2758596224443823643L;

    public ContainerStartupFailedException(Throwable exception)
    {
        super(exception.getMessage(), exception);
    }
}
