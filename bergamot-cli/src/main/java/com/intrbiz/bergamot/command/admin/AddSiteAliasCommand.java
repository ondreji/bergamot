package com.intrbiz.bergamot.command.admin;

import java.util.List;

import com.intrbiz.bergamot.BergamotCLI;
import com.intrbiz.bergamot.BergamotCLICommand;
import com.intrbiz.bergamot.BergamotCLIException;
import com.intrbiz.bergamot.config.UICfg;
import com.intrbiz.bergamot.data.BergamotDB;
import com.intrbiz.bergamot.model.Site;
import com.intrbiz.data.DataManager;
import com.intrbiz.util.pool.database.DatabasePool;

public class AddSiteAliasCommand extends BergamotCLICommand
{
    public AddSiteAliasCommand()
    {
        super();
    }

    @Override
    public String name()
    {
        return "add-site-alias";
    }
    
    @Override
    public boolean admin()
    {
        return true;
    }

    @Override
    public String usage()
    {
        return "<site-name> <site-alias> [<site-alias>, ...]";
    }

    @Override
    public String help()
    {
        return "Add aliases to an existing site\n" +
                "  Eg: bergamot-cli create-site 'bergamot.local' 'bergamot.mydomain.net' 'monitoring.mydomain.com'\n" +
                "\n" +
                "Arguments:\n" +
                "  <site-name> the host name of this Bergamot site, Eg: 'bergamot.local'\n" +
                "  <site-alias> additional host names for this Bergamot site, Eg: 'bergamot.mydomain.com'\n" +
                "\n" +
                "Note: this command must to be run locally on the UI node.";
    }

    @Override
    public int execute(BergamotCLI cli, List<String> args) throws Exception
    {
        if (args.size() < 2) throw new BergamotCLIException("No site name or alias given");
        // site name
        String siteName = args.remove(0);
        // read the UI config and connect to the database
        UICfg config = UICfg.loadConfiguration();
        // setup the data manager
        DataManager.getInstance().registerDefaultServer(DatabasePool.Default.with().postgresql().url(config.getDatabase().getUrl()).username(config.getDatabase().getUsername()).password(config.getDatabase().getPassword()).build());
        // ensure the DB schema is installed
        BergamotDB.install();
        // now actually create the site
        try (BergamotDB db = BergamotDB.connect())
        {
            // ge the site
            Site site = db.getSiteByName(siteName);
            if (site == null) throw new BergamotCLIException("No site exists with the name: '" + siteName + "'");
            // check the aliases do not already exist
            for (String alias : args)
            {
                if (db.getSiteByName(alias) != null) throw new BergamotCLIException("A site already exists with the alias: '" + alias + "'");
            }
            // add the aliases
            for (String alias : args)
            {
                site.getAliases().add(alias);
            }
            db.setSite(site);
        }
        // all ok
        return 0;
    }
}