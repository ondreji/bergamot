package com.intrbiz.bergamot.worker.engine.agent;

import java.util.UUID;

import org.apache.log4j.Logger;

import com.intrbiz.bergamot.agent.server.BergamotAgentServerHandler;
import com.intrbiz.bergamot.model.message.check.ExecuteCheck;
import com.intrbiz.bergamot.model.message.result.ActiveResultMO;
import com.intrbiz.bergamot.worker.engine.AbstractExecutor;
import com.intrbiz.bergamot.worker.engine.agent.script.BergamotAgentScriptWrapper;
import com.intrbiz.bergamot.worker.engine.script.ScriptedCheckManager;

/**
 * Execute a scripted Bergamot check against an agent
 */
public class ScriptExecutor extends AbstractExecutor<AgentEngine>
{
    public static final String NAME = "script";
    
    private static final Logger logger = Logger.getLogger(ScriptExecutor.class);
    
    private final ScriptedCheckManager scriptManager = new ScriptedCheckManager(this);

    public ScriptExecutor()
    {
        super();
    }

    /**
     * Only execute Checks where the engine == "agent"
     */
    @Override
    public boolean accept(ExecuteCheck task)
    {
        return AgentEngine.NAME.equals(task.getEngine()) && NAME.equals(task.getExecutor());
    }

    @Override
    public void execute(ExecuteCheck executeCheck)
    {
        if (logger.isTraceEnabled()) logger.trace("Running script Bergamot Agent check");
        try
        {
            // check the host presence
            UUID agentId = executeCheck.getAgentId();
            if (agentId == null) throw new RuntimeException("No agent id was given");
            // lookup the agent
            BergamotAgentServerHandler agent = this.getEngine().getAgentServer().getRegisteredAgent(agentId);
            if (agent != null)
            {
                // execute the script
                this.scriptManager.createExecutor(executeCheck)
                    .bind("agent", new BergamotAgentScriptWrapper(agent))
                    .execute();
            }
            else
            {
                // raise an error
                this.publishActiveResult(executeCheck, new ActiveResultMO().fromCheck(executeCheck).disconnected("Bergamot Agent disconnected"));
            }
        }
        catch (Exception e)
        {
            this.publishActiveResult(executeCheck, new ActiveResultMO().fromCheck(executeCheck).error(e));
        }
    }
    
}
