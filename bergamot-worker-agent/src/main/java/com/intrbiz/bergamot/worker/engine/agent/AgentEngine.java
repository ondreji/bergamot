package com.intrbiz.bergamot.worker.engine.agent;

import org.apache.log4j.Logger;

import com.intrbiz.bergamot.agent.server.BergamotAgentServer;
import com.intrbiz.bergamot.agent.server.config.BergamotAgentServerCfg;
import com.intrbiz.bergamot.worker.config.AgentWorkerCfg;
import com.intrbiz.bergamot.worker.engine.AbstractEngine;

/**
 * Execute Bergamot Agent checks via the Bergamot Agent Server
 */
public class AgentEngine extends AbstractEngine
{
    public static final String NAME = "agent";
    
    private BergamotAgentServer agentServer;

    public AgentEngine()
    {
        super(NAME);
    }
    
    @Override
    public boolean isAgentRouted()
    {
        return true;
    }

    @Override
    protected void configure() throws Exception
    {
        super.configure();
        // setup executors
        if (this.executors.isEmpty())
        {
            this.addExecutor(new PresenceExecutor());
            this.addExecutor(new CPUExecutor());
            this.addExecutor(new MemoryExecutor());
            this.addExecutor(new DiskExecutor());
        }
    }
    
    @Override
    public void start() throws Exception
    {
        // configure the agent server
        BergamotAgentServerCfg serverCfg = ((AgentWorkerCfg) this.getWorker().getConfiguration()).getAgentServer();
        Logger.getLogger(AgentEngine.class).info("Listening for Bergamot Agent connections on port: " + serverCfg.getPort());
        this.agentServer = new BergamotAgentServer();
        this.agentServer.configure(serverCfg);
        // handle binding / unbinding agent routes when an agent connects and disconnects
        this.agentServer.setOnAgentRegisterHandler((handler) -> {
            this.bindAgent(handler.getHello().getHostId());
        });
        this.agentServer.setOnAgentUnregisterHandler((handler) -> {
            // unbind the agent from this worker so that active checks will not be routed to us
            this.unbindAgent(handler.getHello().getHostId());
        });
        // setup queues etc
        super.start();
        // start the agent server
        this.agentServer.start();
    }

    public BergamotAgentServer getAgentServer()
    {
        return this.agentServer;
    }
}
