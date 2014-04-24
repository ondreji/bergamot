package com.intrbiz.bergamot.model;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import com.intrbiz.Util;
import com.intrbiz.bergamot.compat.config.model.ServicegroupCfg;
import com.intrbiz.bergamot.model.state.GroupState;

public class ServiceGroup extends Group
{
    private Map<UUID, Service> services = new TreeMap<UUID, Service>();

    public ServiceGroup()
    {
        super();
    }

    public void configure(ServicegroupCfg config)
    {
        this.name = config.resolveServicegroupName();
        this.displayName = Util.coalesceEmpty(config.resolveAlias(), this.name);
    }

    public Collection<Service> getServices()
    {
        return this.services.values();
    }

    public Service getService(UUID id)
    {
        return this.services.get(id);
    }

    public boolean containsService(UUID id)
    {
        return this.services.containsKey(id);
    }

    public int getServiceCount()
    {
        return this.services.size();
    }

    public void addService(Service service)
    {
        this.services.put(service.getId(), service);
        service.addServicegroup(this);
    }
    
    @Override
    public GroupState getState()
    {
        return GroupState.compute(this.getServices(), null);
    }
}
