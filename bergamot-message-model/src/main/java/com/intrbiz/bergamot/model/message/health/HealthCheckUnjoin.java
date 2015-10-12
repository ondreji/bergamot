package com.intrbiz.bergamot.model.message.health;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.intrbiz.bergamot.model.message.MessageObject;

/**
 * Announce a daemons intention to stop heartbeating and leave the cluster
 */
public class HealthCheckUnjoin extends MessageObject
{
    @JsonProperty("instance-id")
    private UUID instanceId;
    
    @JsonProperty("daemon-name")
    private String daemonName;
    
    public HealthCheckUnjoin()
    {
        super();
    }
    
    public HealthCheckUnjoin(UUID instanceId, String daemonName)
    {
        super();
        this.instanceId = instanceId;
        this.daemonName = daemonName;
    }

    public UUID getInstanceId()
    {
        return instanceId;
    }

    public void setInstanceId(UUID instanceId)
    {
        this.instanceId = instanceId;
    }

    public String getDaemonName()
    {
        return daemonName;
    }

    public void setDaemonName(String daemonName)
    {
        this.daemonName = daemonName;
    }
}
