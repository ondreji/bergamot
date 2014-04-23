package com.intrbiz.bergamot.model.message.result;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.intrbiz.bergamot.model.Status;
import com.intrbiz.bergamot.model.message.Message;
import com.intrbiz.bergamot.model.message.task.Check;
import com.intrbiz.bergamot.model.util.Parameter;

/**
 * The result of a check
 */
@JsonTypeName("bergamot.result")
public class Result extends Message
{
    @JsonProperty("checkable_type")
    private String checkableType;

    @JsonProperty("checkable_id")
    private UUID checkableId;

    @JsonProperty("check")
    private Check check;

    @JsonProperty("ok")
    private boolean ok;

    @JsonProperty("status")
    private Status status;

    @JsonProperty("executed")
    private long executed;

    @JsonProperty("processed")
    private long processed;

    @JsonProperty("runtime")
    private double runtime;

    @JsonProperty("output")
    private String output;

    @JsonProperty("parameters")
    private List<Parameter> parameters = new LinkedList<Parameter>();

    public Result()
    {
        super();
    }

    public String getCheckableType()
    {
        return checkableType;
    }

    public void setCheckableType(String checkableType)
    {
        this.checkableType = checkableType;
    }

    public UUID getCheckableId()
    {
        return checkableId;
    }

    public void setCheckableId(UUID checkableId)
    {
        this.checkableId = checkableId;
    }

    public Check getCheck()
    {
        return check;
    }

    public void setCheck(Check check)
    {
        this.check = check;
    }

    public boolean isOk()
    {
        return ok;
    }

    public void setOk(boolean ok)
    {
        this.ok = ok;
    }

    public Status getStatus()
    {
        return status;
    }

    public void setStatus(Status status)
    {
        this.status = status;
    }

    public long getExecuted()
    {
        return executed;
    }

    public void setExecuted(long executed)
    {
        this.executed = executed;
    }

    public long getProcessed()
    {
        return processed;
    }

    public void setProcessed(long processed)
    {
        this.processed = processed;
    }

    public double getRuntime()
    {
        return runtime;
    }

    public void setRuntime(double runtime)
    {
        this.runtime = runtime;
    }

    public String getOutput()
    {
        return output;
    }

    public void setOutput(String output)
    {
        this.output = output;
    }

    public List<Parameter> getParameters()
    {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters)
    {
        this.parameters = parameters;
    }

    public void addParameter(String name, String value)
    {
        this.parameters.add(new Parameter(name, value));
    }

    public void setParameter(String name, String value)
    {
        this.removeParameter(name);
        this.addParameter(name, value);
    }

    public void removeParameter(String name)
    {
        for (Iterator<Parameter> i = this.parameters.iterator(); i.hasNext();)
        {
            if (name.equals(i.next().getName()))
            {
                i.remove();
                break;
            }
        }
    }

    public void clearParameters()
    {
        this.parameters.clear();
    }

    public String getParameter(String name)
    {
        return this.getParameter(name, null);
    }

    public String getParameter(String name, String defaultValue)
    {
        for (Parameter parameter : this.parameters)
        {
            if (name.equals(parameter.getName())) return parameter.getValue();
        }
        return defaultValue;
    }
}
