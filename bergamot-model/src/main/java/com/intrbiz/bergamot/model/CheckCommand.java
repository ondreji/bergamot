package com.intrbiz.bergamot.model;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.stream.Collectors;

import com.intrbiz.Util;
import com.intrbiz.bergamot.config.model.CheckCommandCfg;
import com.intrbiz.bergamot.data.BergamotDB;
import com.intrbiz.bergamot.model.adapter.ParametersAdapter;
import com.intrbiz.bergamot.model.message.CheckCommandMO;
import com.intrbiz.bergamot.model.util.Parameter;
import com.intrbiz.bergamot.model.util.Parameterised;
import com.intrbiz.configuration.CfgParameter;
import com.intrbiz.data.db.compiler.meta.Action;
import com.intrbiz.data.db.compiler.meta.SQLColumn;
import com.intrbiz.data.db.compiler.meta.SQLForeignKey;
import com.intrbiz.data.db.compiler.meta.SQLPrimaryKey;
import com.intrbiz.data.db.compiler.meta.SQLTable;
import com.intrbiz.data.db.compiler.meta.SQLVersion;

/**
 * The definition of a command which is used to check something
 */
@SQLTable(schema = BergamotDB.class, name = "check_command", since = @SQLVersion({ 1, 0, 0 }))
public class CheckCommand extends BergamotObject<CheckCommandMO> implements Parameterised
{
    private static final long serialVersionUID = 1L;
    
    @SQLColumn(index = 1, name = "check_id", since = @SQLVersion({ 1, 0, 0 }))
    @SQLPrimaryKey
    private UUID checkId;

    @SQLColumn(index = 2, name = "command_id", since = @SQLVersion({ 1, 0, 0 }))
    @SQLForeignKey(references = Command.class, on = "id", onDelete = Action.RESTRICT, onUpdate = Action.RESTRICT, since = @SQLVersion({ 1, 6, 0 }))
    private UUID commandId;

    @SQLColumn(index = 3, name = "parameters", type = "JSON", adapter = ParametersAdapter.class, since = @SQLVersion({ 1, 0, 0 }))
    private LinkedHashMap<String, Parameter> parameters = new LinkedHashMap<String, Parameter>();
    
    @SQLColumn(index = 4, name = "script", since = @SQLVersion({ 3, 43, 0 }))
    private String script;

    public CheckCommand()
    {
        super();
    }

    public UUID getCheckId()
    {
        return checkId;
    }

    public void setCheckId(UUID checkId)
    {
        this.checkId = checkId;
    }

    public UUID getCommandId()
    {
        return commandId;
    }

    public void setCommandId(UUID commandId)
    {
        this.commandId = commandId;
    }
    
    public Command getCommand()
    {
        try (BergamotDB db = BergamotDB.connect())
        {
            return db.getCommand(this.getCommandId());
        }
    }

    public void configure(CheckCommandCfg cfg)
    {
        // script
        this.setScript(cfg.getScript());
        // load the parameters
        this.clearParameters();
        for (CfgParameter cp : cfg.getParameters())
        {
            this.addParameter(cp.getName(), cp.getValueOrText());
        }
    }

    @Override
    public LinkedHashMap<String, Parameter> getParameters()
    {
        return parameters;
    }

    @Override
    public void setParameters(LinkedHashMap<String, Parameter> parameters)
    {
        this.parameters = parameters;
    }
    
    public String getScript()
    {
        return script;
    }

    public void setScript(String script)
    {
        this.script = script;
    }

    /**
     * Resolve the parameters between the command and this check definition
     * @return the check parameters
     */
    public LinkedHashMap<String, Parameter> resolveCheckParameters()
    {
        return this.resolveCheckParameters(this.getCommand());
    }
    
    public LinkedHashMap<String, Parameter> resolveCheckParameters(Command command)
    {
        LinkedHashMap<String, Parameter> r = new LinkedHashMap<String, Parameter>();
        if (command != null && command.getParameters() != null)
        {
            r.putAll(command.getParameters());
        }
        if (this.getParameters() != null)
        {
            r.putAll(this.getParameters());
        }
        return r;
    }
    
    /**
     * Find the resolved value of a check parameter
     * @return the check parameter
     */
    public String resolveCheckParameter(String name)
    {
        return this.resolveCheckParameter(this.getCommand(), name);
    }
    
    public String resolveCheckParameter(Command command, String name)
    {
        // check our parameters first
        String value = this.getParameter(name);
        if (value == null)
        {
            if (command != null)
            {
                value = command.getParameter(name);
            }
        }
        return value;
    }
    
    /**
     * Resolve the check script between the command and this check definition
     * @return the check script
     */
    public String resolveCheckScript()
    {
        Command command = this.getCommand();
        return command == null ? this.script : Util.coalesceEmpty(this.script, command.getScript());
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((checkId == null) ? 0 : checkId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        CheckCommand other = (CheckCommand) obj;
        if (checkId == null)
        {
            if (other.checkId != null) return false;
        }
        else if (!checkId.equals(other.checkId)) return false;
        return true;
    }

    @Override
    public CheckCommandMO toMO(Contact contact, EnumSet<MOFlag> options)
    {
        CheckCommandMO mo = new CheckCommandMO();
        Command command = this.getCommand();
        if (command != null)
        {
            if (contact == null || contact.hasPermission("read", command)) mo.setCommand(command.toStubMO(contact));
        }
        mo.setParameters(this.getParameters().entrySet().stream().map((x) -> x.getValue().toMO(contact)).collect(Collectors.toList()));
        mo.setScript(this.getScript());
        return mo;
    }
    
    public String toString()
    {
        return "CheckCommand { command => " + this.getCommand() +  ", parameters => " + this.getParameters() + ", script => " + this.getScript() + "}"; 
    }
}
