package com.intrbiz.bergamot.ui.api;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.intrbiz.Util;
import com.intrbiz.balsa.engine.route.Router;
import com.intrbiz.balsa.error.http.BalsaNotFound;
import com.intrbiz.balsa.metadata.WithDataAdapter;
import com.intrbiz.bergamot.config.model.HostCfg;
import com.intrbiz.bergamot.data.BergamotDB;
import com.intrbiz.bergamot.model.Host;
import com.intrbiz.bergamot.model.Service;
import com.intrbiz.bergamot.model.Site;
import com.intrbiz.bergamot.model.Trap;
import com.intrbiz.bergamot.model.message.HostMO;
import com.intrbiz.bergamot.model.message.ServiceMO;
import com.intrbiz.bergamot.model.message.TrapMO;
import com.intrbiz.bergamot.model.message.state.CheckStateMO;
import com.intrbiz.bergamot.ui.BergamotApp;
import com.intrbiz.metadata.AsUUID;
import com.intrbiz.metadata.Get;
import com.intrbiz.metadata.JSON;
import com.intrbiz.metadata.Prefix;
import com.intrbiz.metadata.RequireValidPrincipal;
import com.intrbiz.metadata.Var;
import com.intrbiz.metadata.XML;


@Prefix("/api/host")
@RequireValidPrincipal()
public class HostAPIRouter extends Router<BergamotApp>
{
    @Get("/")
    @JSON
    @WithDataAdapter(BergamotDB.class)
    public List<HostMO> getHosts(BergamotDB db, @Var("site") Site site)
    {
        return db.listHosts(site.getId()).stream().map(Host::toStubMO).collect(Collectors.toList());
    }
    
    @Get("/name/:name")
    @JSON(notFoundIfNull = true)
    @WithDataAdapter(BergamotDB.class)
    public HostMO getHost(BergamotDB db, @Var("site") Site site, String name)
    {
        return Util.nullable(db.getHostByName(site.getId(), name), Host::toMO);
    }
    
    @Get("/name/:name/state")
    @JSON(notFoundIfNull = true)
    @WithDataAdapter(BergamotDB.class)
    public CheckStateMO getHostState(BergamotDB db, @Var("site") Site site, String name)
    {
        return Util.nullable(db.getHostByName(site.getId(), name), (h)->{return h.getState().toMO();});
    }
    
    @Get("/id/:id")
    @JSON(notFoundIfNull = true)
    @WithDataAdapter(BergamotDB.class)
    public HostMO getHost(BergamotDB db, @AsUUID UUID id)
    {
        return Util.nullable(db.getHost(id), Host::toMO);
    }
    
    @Get("/id/:id/state")
    @JSON(notFoundIfNull = true)
    @WithDataAdapter(BergamotDB.class)
    public CheckStateMO getHostState(BergamotDB db, @AsUUID UUID id)
    {
        return Util.nullable(db.getHost(id), (h)->{return h.getState().toMO();});
    }
    
    @Get("/name/:name/services")
    @JSON(notFoundIfNull = true)
    @WithDataAdapter(BergamotDB.class)
    public List<ServiceMO> getHostServices(BergamotDB db, @Var("site") Site site, String name)
    {
        return Util.nullable(db.getHostByName(site.getId(), name), (h) -> {return h.getServices().stream().map(Service::toMO).collect(Collectors.toList());});
    }
    
    @Get("/id/:id/services")
    @JSON(notFoundIfNull = true)
    @WithDataAdapter(BergamotDB.class)
    public List<ServiceMO> getHostServices(BergamotDB db, @AsUUID UUID id)
    {
        return Util.nullable(db.getHost(id), (e)->{return e.getServices().stream().map(Service::toMO).collect(Collectors.toList());});
    }
    
    @Get("/name/:name/traps")
    @JSON(notFoundIfNull = true)
    @WithDataAdapter(BergamotDB.class)
    public List<TrapMO> getHostTraps(BergamotDB db, @Var("site") Site site, String name)
    {
        return Util.nullable(db.getHostByName(site.getId(), name), (h)->{return h.getTraps().stream().map(Trap::toMO).collect(Collectors.toList());});
    }
    
    @Get("/id/:id/traps")
    @JSON(notFoundIfNull = true)
    @WithDataAdapter(BergamotDB.class)
    public List<TrapMO> getHostTraps(BergamotDB db, @AsUUID UUID id)
    {
        return Util.nullable(db.getHost(id), (e)->{return e.getTraps().stream().map(Trap::toMO).collect(Collectors.toList());});
    }
    
    @Get("/id/:id/execute")
    @JSON()
    @WithDataAdapter(BergamotDB.class)
    public String executeHost(BergamotDB db, @AsUUID UUID id)
    { 
        Host host = db.getHost(id);
        if (host == null) throw new BalsaNotFound("No host with id '" + id + "' exists.");
        action("execute-check", host);
        return "Ok";
    }
    
    @Get("/id/:id/execute-services")
    @JSON()
    @WithDataAdapter(BergamotDB.class)
    public String executeServicesOnHost(BergamotDB db, @AsUUID UUID id)
    { 
        Host host = db.getHost(id);
        if (host == null) throw new BalsaNotFound("No host with id '" + id + "' exists.");
        int executed = 0;
        for (Service service : host.getServices())
        {
            action("execute-check", service);
            executed++;
        }
        return "Ok, executed " + executed + " services";
    }
    
    @Get("/id/:id/suppress-services")
    @JSON()
    @WithDataAdapter(BergamotDB.class)
    public String suppressServicesOnHost(BergamotDB db, @AsUUID UUID id)
    { 
        Host host = db.getHost(id);
        if (host == null) throw new BalsaNotFound("No host with id '" + id + "' exists.");
        int suppressed = 0;
        for (Service service : host.getServices())
        {
            service.setSuppressed(true);
            db.setService(service);
            suppressed++;
        }
        return "Ok, suppressed " + suppressed + " services";
    }
    
    @Get("/id/:id/unsuppress-services")
    @JSON()
    @WithDataAdapter(BergamotDB.class)
    public String unsuppressServicesOnHost(BergamotDB db, @AsUUID UUID id)
    { 
        Host host = db.getHost(id);
        if (host == null) throw new BalsaNotFound("No host with id '" + id + "' exists.");
        int unsuppressed = 0;
        for (Service service : host.getServices())
        {
            service.setSuppressed(false);
            db.setService(service);
            unsuppressed++;
        }
        return "Ok, unsuppressed " + unsuppressed + " services";
    }
    
    @Get("/name/:name/config.xml")
    @XML(notFoundIfNull = true)
    @WithDataAdapter(BergamotDB.class)
    public HostCfg getHostConfig(BergamotDB db, @Var("site") Site site, String name)
    {
        return Util.nullable(db.getHostByName(site.getId(), name), Host::getConfiguration);
    }
    
    @Get("/id/:id/config.xml")
    @XML(notFoundIfNull = true)
    @WithDataAdapter(BergamotDB.class)
    public HostCfg getHostConfig(BergamotDB db, @AsUUID UUID id)
    {
        return Util.nullable(db.getHost(id), Host::getConfiguration);
    }
}
