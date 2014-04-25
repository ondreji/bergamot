package com.intrbiz.bergamot.worker.engine;

import com.intrbiz.bergamot.config.ExecutorCfg;
import com.intrbiz.bergamot.worker.Engine;
import com.intrbiz.bergamot.worker.Executor;

public abstract class AbstractExecutor<T extends Engine> implements Executor<T>
{
    protected T Engine;

    protected ExecutorCfg config;

    public AbstractExecutor()
    {
        super();
    }

    @Override
    public T getEngine()
    {
        return this.Engine;
    }

    @Override
    public void setEngine(T engine)
    {
        this.Engine = engine;
    }

    @Override
    public void configure(ExecutorCfg config) throws Exception
    {
        this.config = config;
        this.configure();
    }

    @Override
    public ExecutorCfg getConfiguration()
    {
        return this.config;
    }

    protected void configure() throws Exception
    {
    }
}