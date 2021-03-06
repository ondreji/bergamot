package com.intrbiz.bergamot.ui.router;

import java.io.IOException;
import java.util.UUID;

import com.intrbiz.balsa.engine.route.Router;
import com.intrbiz.balsa.metadata.WithDataAdapter;
import com.intrbiz.bergamot.data.BergamotDB;
import com.intrbiz.bergamot.metadata.GetBergamotSite;
import com.intrbiz.bergamot.metadata.IsaObjectId;
import com.intrbiz.bergamot.model.Site;
import com.intrbiz.bergamot.model.Trap;
import com.intrbiz.bergamot.ui.BergamotApp;
import com.intrbiz.metadata.Any;
import com.intrbiz.metadata.Prefix;
import com.intrbiz.metadata.RequireValidPrincipal;
import com.intrbiz.metadata.Template;

@Prefix("/trap")
@Template("layout/main")
@RequireValidPrincipal()
public class TrapRouter extends Router<BergamotApp>
{    
    @Any("/name/:host/:trap")
    @WithDataAdapter(BergamotDB.class)
    public void trap(BergamotDB db, String hostName, String trapName, @GetBergamotSite() Site site)
    {
        Trap trap = model("trap", notNull(db.getTrapOnHostByName(site.getId(), hostName, trapName)));
        require(permission("read", trap));
        model("alerts", db.getAllAlertsForCheck(trap.getId(), 3, 0));
        encode("trap/detail");
    }
    
    @Any("/id/:id")
    @WithDataAdapter(BergamotDB.class)
    public void trap(BergamotDB db, @IsaObjectId UUID id)
    {
        Trap trap = model("trap", notNull(db.getTrap(id)));
        require(permission("read", trap));
        model("alerts", db.getAllAlertsForCheck(id, 3, 0));
        encode("trap/detail");
    }
    
    @Any("/enable/:id")
    @WithDataAdapter(BergamotDB.class)
    public void enableTrap(BergamotDB db, @IsaObjectId UUID id) throws IOException
    {
        Trap trap = notNull(db.getTrap(id));
        require(permission("enable", trap));
        trap.setEnabled(true);
        db.setTrap(trap);
        redirect("/trap/id/" + id);
    }
    
    @Any("/disable/:id")
    @WithDataAdapter(BergamotDB.class)
    public void disableTrap(BergamotDB db, @IsaObjectId UUID id) throws IOException
    {
        Trap trap = notNull(db.getTrap(id));
        require(permission("disable", trap));
        trap.setEnabled(false);
        db.setTrap(trap);
        redirect("/trap/id/" + id);
    }
    
    @Any("/suppress/:id")
    @WithDataAdapter(BergamotDB.class)
    public void suppressTrap(BergamotDB db, @IsaObjectId UUID id) throws IOException
    {
        Trap trap = notNull(db.getTrap(id));
        require(permission("suppress", trap));
        action("suppress-check", trap);
        redirect("/trap/id/" + id);
    }
    
    @Any("/unsuppress/:id")
    @WithDataAdapter(BergamotDB.class)
    public void unsuppressTrap(BergamotDB db, @IsaObjectId UUID id) throws IOException
    {
        Trap trap = notNull(db.getTrap(id));
        require(permission("unsuppress", trap));
        action("unsuppress-check", trap);
        redirect("/trap/id/" + id);
    }
}
