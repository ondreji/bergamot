package com.intrbiz.bergamot.model.message.result;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.intrbiz.bergamot.model.message.Message;
import com.intrbiz.bergamot.model.message.ParameterMO;

/**
 * The result of a check
 */
public abstract class ResultMO extends Message
{    
    @JsonProperty("ok")
    private boolean ok;

    @JsonProperty("status")
    private String status;

    @JsonProperty("executed")
    private long executed;

    @JsonProperty("processed")
    private long processed;

    @JsonProperty("runtime")
    private double runtime;

    @JsonProperty("output")
    private String output;

    @JsonProperty("parameters")
    private List<ParameterMO> parameters = new LinkedList<ParameterMO>();

    public ResultMO()
    {
        super();
    }

    public boolean isOk()
    {
        return ok;
    }

    public void setOk(boolean ok)
    {
        this.ok = ok;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
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

    public List<ParameterMO> getParameters()
    {
        return parameters;
    }

    public void setParameters(List<ParameterMO> parameters)
    {
        this.parameters = parameters;
    }

    public void addParameter(String name, String value)
    {
        this.parameters.add(new ParameterMO(name, value));
    }

    public void setParameter(String name, String value)
    {
        this.removeParameter(name);
        this.addParameter(name, value);
    }

    public void removeParameter(String name)
    {
        for (Iterator<ParameterMO> i = this.parameters.iterator(); i.hasNext();)
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
        for (ParameterMO parameter : this.parameters)
        {
            if (name.equals(parameter.getName())) return parameter.getValue();
        }
        return defaultValue;
    }
    
    // constructor helpers
    
    @JsonIgnore
    public ResultMO pending(String output)
    {
        this.setOk(true);
        this.setStatus("PENDING");
        this.setOutput(output);
        this.setRuntime(0);
        return this;
    }
    
    
    @JsonIgnore
    public ResultMO info(String message)
    {
        this.setOk(true);
        this.setStatus("INFO");
        this.setOutput(message);
        this.setRuntime(0);
        return this;
    }
    
    @JsonIgnore
    public ResultMO ok(String output)
    {
        this.setOk(true);
        this.setStatus("OK");
        this.setOutput(output);
        this.setRuntime(0);
        return this;
    }
    
    @JsonIgnore
    public ResultMO warning(String output)
    {
        this.setOk(false);
        this.setStatus("WARNING");
        this.setOutput(output);
        this.setRuntime(0);
        return this;
    }
    
    @JsonIgnore
    public ResultMO critical(String output)
    {
        this.setOk(false);
        this.setStatus("CRITICAL");
        this.setOutput(output);
        this.setRuntime(0);
        return this;
    }
    
    @JsonIgnore
    public ResultMO unknown(String output)
    {
        this.setOk(false);
        this.setStatus("UNKNOWN");
        this.setOutput(output);
        this.setRuntime(0);
        return this;
    }
    
    /**
     * Update this result with the error information
     * @param t
     * @return
     */
    @JsonIgnore
    public ResultMO error(Throwable t)
    {
        this.setOk(false);
        this.setStatus("ERROR");
        this.setOutput(t.getMessage());
        this.setRuntime(0);
        return this;
    }
    
    @JsonIgnore
    public ResultMO error(String message)
    {
        this.setOk(false);
        this.setStatus("ERROR");
        this.setOutput(message);
        this.setRuntime(0);
        return this;
    }
    
    @JsonIgnore
    public ResultMO timeout(String message)
    {
        this.setOk(false);
        this.setStatus("TIMEOUT");
        this.setOutput(message);
        this.setRuntime(0);
        return this;
    }
    
    @JsonIgnore
    public ResultMO disconnected(String message)
    {
        this.setOk(false);
        this.setStatus("DISCONNECTED");
        this.setOutput(message);
        this.setRuntime(0);
        return this;
    }
    
    @JsonIgnore
    public ResultMO action(String message)
    {
        this.setOk(false);
        this.setStatus("ACTION");
        this.setOutput(message);
        this.setRuntime(0);
        return this;
    }
    
    /**
     * Apply a warning / critical threshold to determine the result state, this will 
     * result in either a ok, warning or critical state depending on the value and 
     * thresholds
     * 
     * @param value the value to check
     * @param warning the warning threshold
     * @param critical the critical threshold
     * @param message the check output
     */
    @JsonIgnore
    public ResultMO applyThreshold(double value, double warning, double critical, String message)
    {
        if (value > critical)
        {
            this.critical(message);
        }
        else if (value > warning)
        {
            this.warning(message);
        }
        else
        {
            this.ok(message);
        }
        return this;
    }
    
    /**
     * Apply a warning / critical threshold to determine the result state, this will 
     * result in either a ok, warning or critical state depending on the value and 
     * thresholds
     * 
     * @param value the value to check
     * @param warning the warning threshold
     * @param critical the critical threshold
     * @param message the check output
     */
    @JsonIgnore
    public ResultMO applyThreshold(long value, long warning, long critical, String message)
    {
        if (value > critical)
        {
            this.critical(message);
        }
        else if (value > warning)
        {
            this.warning(message);
        }
        else
        {
            this.ok(message);
        }
        return this;
    }
    
    /**
     * Apply a warning / critical threshold to determine the result state, this will 
     * result in either a ok, warning or critical state depending on the values and 
     * thresholds.
     * 
     * This applies a threshold check to a collection of values, with the worst status 
     * being the status of this result.
     * 
     * @param values the values to check
     * @param warning the warning threshold
     * @param critical the critical threshold
     * @param message the check output
     */
    @JsonIgnore
    public ResultMO applyThreshold(Iterable<Long> values, long warning, long critical, String message)
    {
        int state = 0;
        for (long value : values)
        {
            if (value > critical)
            {
                state = Math.max(state, 2);
            }
            else if (value > warning)
            {
                state = Math.max(state, 1);
            }
            else
            {
                state = Math.max(state, 0);
            }
        }
        switch (state)
        {
            case 0:
                this.ok(message);
                break;
            case 1:
                this.warning(message);
            case 2:
                this.critical(message);
        }
        return this;
    }
    
    /**
     * Apply a warning / critical threshold to determine the result state, this will 
     * result in either a ok, warning or critical state depending on the values and 
     * thresholds.
     * 
     * This applies a threshold check to a collection of values, with the worst status 
     * being the status of this result.
     * 
     * @param values the values to check
     * @param warning the warning threshold
     * @param critical the critical threshold
     * @param message the check output
     */
    @JsonIgnore
    public ResultMO applyThreshold(Iterable<Double> values, double warning, double critical, String message)
    {
        int state = 0;
        for (double value : values)
        {
            if (value > critical)
            {
                state = Math.max(state, 2);
            }
            else if (value > warning)
            {
                state = Math.max(state, 1);
            }
            else
            {
                state = Math.max(state, 0);
            }
        }
        switch (state)
        {
            case 0:
                this.ok(message);
                break;
            case 1:
                this.warning(message);
            case 2:
                this.critical(message);
        }
        return this;
    }
    
    public ResultMO runtime(double runtime)
    {
        this.runtime = runtime;
        return this;
    }
    
    /**
     * Apply a warning / critical range to determine the result state, this will 
     * result in either a ok, warning or critical state depending on the value and 
     * ranges
     * 
     * @param value the value to check
     * @param warning the warning range
     * @param critical the critical range
     * @param message the check output
     */
    @JsonIgnore
    public ResultMO applyRange(long value, long[] warning, long[] critical, String message)
    {
        if (value < critical[0] || value > critical[1])
        {
            this.critical(message);
        }
        else if (value < warning[0] || value > warning[1])
        {
            this.warning(message);
        }
        else
        {
            this.ok(message);
        }
        return this;
    }
    
    /**
     * Apply a warning / critical range to determine the result state, this will 
     * result in either a ok, warning or critical state depending on the value and 
     * ranges
     * 
     * @param value the value to check
     * @param warning the warning range
     * @param critical the critical range
     * @param message the check output
     */
    @JsonIgnore
    public ResultMO applyRange(int value, int[] warning, int[] critical, String message)
    {
        if (value < critical[0] || value > critical[1])
        {
            this.critical(message);
        }
        else if (value < warning[0] || value > warning[1])
        {
            this.warning(message);
        }
        else
        {
            this.ok(message);
        }
        return this;
    }
    
    /**
     * Apply a warning / critical range to determine the result state, this will 
     * result in either a ok, warning or critical state depending on the value and 
     * ranges
     * 
     * @param value the value to check
     * @param warning the warning range
     * @param critical the critical range
     * @param message the check output
     */
    @JsonIgnore
    public ResultMO applyRange(float value, float[] warning, float[] critical, String message)
    {
        if (value < critical[0] || value > critical[1])
        {
            this.critical(message);
        }
        else if (value < warning[0] || value > warning[1])
        {
            this.warning(message);
        }
        else
        {
            this.ok(message);
        }
        return this;
    }
    
    /**
     * Apply a warning / critical range to determine the result state, this will 
     * result in either a ok, warning or critical state depending on the value and 
     * ranges
     * 
     * @param value the value to check
     * @param warning the warning range
     * @param critical the critical range
     * @param message the check output
     */
    @JsonIgnore
    public ResultMO applyRange(double value, double[] warning, double[] critical, String message)
    {
        if (value < critical[0] || value > critical[1])
        {
            this.critical(message);
        }
        else if (value < warning[0] || value > warning[1])
        {
            this.warning(message);
        }
        else
        {
            this.ok(message);
        }
        return this;
    }
}
