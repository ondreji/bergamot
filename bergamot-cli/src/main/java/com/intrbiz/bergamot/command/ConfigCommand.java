package com.intrbiz.bergamot.command;

import java.util.List;

import com.intrbiz.bergamot.BergamotCLI;
import com.intrbiz.bergamot.BergamotCLICommand;
import com.intrbiz.bergamot.BergamotCLIException;
import com.intrbiz.bergamot.config.CLICfg;
import com.intrbiz.bergamot.config.CLISiteCfg;

public class ConfigCommand extends BergamotCLICommand
{
    public ConfigCommand()
    {
        super();
    }

    @Override
    public String name()
    {
        return "config";
    }

    @Override
    public String usage()
    {
        return "(show|add-site|remove-site) ...";
    }

    @Override
    public String help()
    {
        return "Configure the Bergamot CLI\n" +
                "\n" +
                "Commands:\n" +
                "  show                                                     Show the current CLI configuration\n" +
                "  add-site <site-name> <site-url> <username> <password>    Add or update a site in the CLI configuration\n" +
                "  remove-site <site-name>                                  Remove a configured site from the CLI configuration'\n" +
                "\n";
    }

    @Override
    public int execute(BergamotCLI cli, List<String> args) throws Exception
    {
        if (args.isEmpty())
        {
            this.showConfig();
        }
        else
        {
            String subCommand = args.remove(0);
            if ("show".equals(subCommand))
            {
                this.showConfig();
            }
            else if ("add-site".equals(subCommand))
            {
                if (args.size() != 4) throw new BergamotCLIException("Add site expects 4 arguments: <name> <url> <username> <password>");
                String name = args.remove(0);
                String url = args.remove(0);
                String username = args.remove(0);
                String password = args.remove(0);
                //
                CLICfg cfg = CLICfg.loadConfiguration();
                cfg.setSite(new CLISiteCfg(name, url, username, password));
                cfg.saveConfiguration();
                System.out.println(cfg);
            }
            else if ("remove-site".equals(subCommand))
            {
                if (args.size() != 1) throw new BergamotCLIException("Add site expects 1 argument: <name>");
                String name = args.remove(0);
                //
                CLICfg cfg = CLICfg.loadConfiguration();
                cfg.removeSite(name);
                cfg.saveConfiguration();
                System.out.println(cfg);
            }
        }
        return 0;
    }
    
    private void showConfig() throws Exception
    {
        CLICfg config = CLICfg.loadConfiguration();
        System.out.println(config.toString());
    }
}
