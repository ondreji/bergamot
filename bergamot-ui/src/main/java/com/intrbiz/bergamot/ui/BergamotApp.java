package com.intrbiz.bergamot.ui;

import java.util.UUID;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.intrbiz.accounting.AccountingManager;
import com.intrbiz.balsa.BalsaApplication;
import com.intrbiz.balsa.engine.impl.session.HazelcastSessionEngine;
import com.intrbiz.balsa.util.Util;
import com.intrbiz.bergamot.accounting.BergamotAccountingQueueConsumer;
import com.intrbiz.bergamot.accounting.consumer.BergamotLoggingConsumer;
import com.intrbiz.bergamot.cluster.ClusterManager;
import com.intrbiz.bergamot.config.UICfg;
import com.intrbiz.bergamot.data.BergamotDB;
import com.intrbiz.bergamot.health.HealthAgent;
import com.intrbiz.bergamot.health.HealthTracker;
import com.intrbiz.bergamot.model.Site;
import com.intrbiz.bergamot.queue.util.QueueUtil;
import com.intrbiz.bergamot.ui.action.BergamotAgentActions;
import com.intrbiz.bergamot.ui.action.CheckActions;
import com.intrbiz.bergamot.ui.action.ConfigChangeActions;
import com.intrbiz.bergamot.ui.action.ContactActions;
import com.intrbiz.bergamot.ui.action.DispatchResultAction;
import com.intrbiz.bergamot.ui.action.ExecuteCheckAction;
import com.intrbiz.bergamot.ui.action.SchedulerActions;
import com.intrbiz.bergamot.ui.action.TeamActions;
import com.intrbiz.bergamot.ui.action.TimePeriodActions;
import com.intrbiz.bergamot.ui.action.U2FAActions;
import com.intrbiz.bergamot.ui.api.APIRouter;
import com.intrbiz.bergamot.ui.api.AgentAPIRouter;
import com.intrbiz.bergamot.ui.api.AlertsAPIRouter;
import com.intrbiz.bergamot.ui.api.ClusterAPIRouter;
import com.intrbiz.bergamot.ui.api.CommandAPIRouter;
import com.intrbiz.bergamot.ui.api.CommentsAPIRouter;
import com.intrbiz.bergamot.ui.api.ConfigAPIRouter;
import com.intrbiz.bergamot.ui.api.ContactAPIRouter;
import com.intrbiz.bergamot.ui.api.DowntimeAPIRouter;
import com.intrbiz.bergamot.ui.api.GroupAPIRouter;
import com.intrbiz.bergamot.ui.api.HostAPIRouter;
import com.intrbiz.bergamot.ui.api.LamplighterAPIRouter;
import com.intrbiz.bergamot.ui.api.LocationAPIRouter;
import com.intrbiz.bergamot.ui.api.MetricsAPIRouter;
import com.intrbiz.bergamot.ui.api.ResourceAPIRouter;
import com.intrbiz.bergamot.ui.api.ServiceAPIRouter;
import com.intrbiz.bergamot.ui.api.StatsAPIRouter;
import com.intrbiz.bergamot.ui.api.TeamAPIRouter;
import com.intrbiz.bergamot.ui.api.TestAPIRouter;
import com.intrbiz.bergamot.ui.api.TimePeriodAPIRouter;
import com.intrbiz.bergamot.ui.api.TrapAPIRouter;
import com.intrbiz.bergamot.ui.api.UtilAPIRouter;
import com.intrbiz.bergamot.ui.express.BergamotCSSVersion;
import com.intrbiz.bergamot.ui.express.BergamotJSVersion;
import com.intrbiz.bergamot.ui.express.BergamotUpdateURL;
import com.intrbiz.bergamot.ui.router.AboutRouter;
import com.intrbiz.bergamot.ui.router.AlertsRouter;
import com.intrbiz.bergamot.ui.router.ClusterRouter;
import com.intrbiz.bergamot.ui.router.CommandRouter;
import com.intrbiz.bergamot.ui.router.ContactRouter;
import com.intrbiz.bergamot.ui.router.DashboardRouter;
import com.intrbiz.bergamot.ui.router.ErrorRouter;
import com.intrbiz.bergamot.ui.router.GroupsRouter;
import com.intrbiz.bergamot.ui.router.HealthRouter;
import com.intrbiz.bergamot.ui.router.HostRouter;
import com.intrbiz.bergamot.ui.router.LocationRouter;
import com.intrbiz.bergamot.ui.router.LoginRouter;
import com.intrbiz.bergamot.ui.router.ProfileRouter;
import com.intrbiz.bergamot.ui.router.ResourceRouter;
import com.intrbiz.bergamot.ui.router.SLARouter;
import com.intrbiz.bergamot.ui.router.ServiceRouter;
import com.intrbiz.bergamot.ui.router.StatsRouter;
import com.intrbiz.bergamot.ui.router.StatusRouter;
import com.intrbiz.bergamot.ui.router.TeamRouter;
import com.intrbiz.bergamot.ui.router.TimePeriodRouter;
import com.intrbiz.bergamot.ui.router.TrapRouter;
import com.intrbiz.bergamot.ui.router.UIRouter;
import com.intrbiz.bergamot.ui.router.admin.AdminRouter;
import com.intrbiz.bergamot.ui.router.admin.ClusterAdminRouter;
import com.intrbiz.bergamot.ui.router.admin.CommandAdminRouter;
import com.intrbiz.bergamot.ui.router.admin.ConfigAdminRouter;
import com.intrbiz.bergamot.ui.router.admin.ConfigChangeAdminRouter;
import com.intrbiz.bergamot.ui.router.admin.ContactAdminRouter;
import com.intrbiz.bergamot.ui.router.admin.CredentialAdminRouter;
import com.intrbiz.bergamot.ui.router.admin.GroupAdminRouter;
import com.intrbiz.bergamot.ui.router.admin.HostAdminRouter;
import com.intrbiz.bergamot.ui.router.admin.LocationAdminRouter;
import com.intrbiz.bergamot.ui.router.admin.ResourceAdminRouter;
import com.intrbiz.bergamot.ui.router.admin.SecurityDomainAdminRouter;
import com.intrbiz.bergamot.ui.router.admin.ServiceAdminRouter;
import com.intrbiz.bergamot.ui.router.admin.TeamAdminRouter;
import com.intrbiz.bergamot.ui.router.admin.TimePeriodAdminRouter;
import com.intrbiz.bergamot.ui.router.admin.TrapAdminRouter;
import com.intrbiz.bergamot.ui.router.agent.AgentRouter;
import com.intrbiz.bergamot.ui.router.command.CommandEditorRouter;
import com.intrbiz.bergamot.ui.router.global.FirstInstallRouter;
import com.intrbiz.bergamot.ui.router.global.GlobalAdminRouter;
import com.intrbiz.bergamot.ui.router.global.GlobalUtilsAdminRouter;
import com.intrbiz.bergamot.ui.security.BergamotSecurityEngine;
import com.intrbiz.bergamot.updater.UpdateServer;
import com.intrbiz.configuration.Configurable;
import com.intrbiz.crypto.SecretKey;
import com.intrbiz.data.DataManager;
import com.intrbiz.data.cache.HazelcastCacheProvider;
import com.intrbiz.lamplighter.data.LamplighterDB;
import com.intrbiz.util.pool.database.DatabasePool;

/**
 * The Bergamot web interface
 */
public class BergamotApp extends BalsaApplication implements Configurable<UICfg>
{   
    public static final class VERSION
    {
        public static final String NAME = "Bergamot Monitoring";
        
        public static final String NUMBER = "3.0.0";
        
        public static final String CODE_NAME = "Red Snow";
        
        public static final class COMPONENTS
        {
        
            public static final String JS = "v1.6.0";
            
            public static final String CSS = "v1.7.4";
        
        }
    }
    
    private UICfg config;
    
    private ClusterManager clusterManager;
    
    private UpdateServer updateServer;
    
    private final UUID id = UUID.randomUUID();
    
    private boolean loggingConfigured = false;
    
    public BergamotApp()
    {
        super();
    }
    
    public UUID id()
    {
        return id;
    }
    
    @Override
    public void configure(UICfg cfg) throws Exception
    {
        this.config = cfg;
    }

    @Override
    public UICfg getConfiguration()
    {
        return this.config;
    }
    
    /**
     * Override the default configuration method
     */
    protected void configureLogging()
    {
        if (! this.loggingConfigured)
        {
            // TODO
            // configure logging to terminal
            BasicConfigurator.configure();
            Logger.getRootLogger().setLevel(Level.toLevel(this.config.getLogging().getLevel().toUpperCase()));
            // only run logging config once
            this.loggingConfigured = true;
        }
    }
    
    protected int getListenerPort(String listenerType, int defaultPort)
    {
        if ("scgi".equalsIgnoreCase(listenerType)) return this.config.getListen().getScgiPort();
        return Integer.getInteger("balsa." + listenerType + ".port", defaultPort);
    }
    
    protected int getListenerThreadCount(String listenerType, int defaultThreadCount)
    {
        if ("scgi".equalsIgnoreCase(listenerType)) return this.config.getListen().getScgiWorkers();
        return Integer.getInteger("balsa." + listenerType + ".workers", Integer.getInteger("balsa.workers", defaultThreadCount));
    }

    @Override
    protected void setupEngines() throws Exception
    {
        // TODO: Don't bother sending metric yet
        // Setup Gerald - Service name: Bergamot.UI, send every minute
        // Gerald.theMole().from(this.getInstanceName()).period(1, TimeUnit.MINUTES);
        // task engine
        /*
         * TODO: disable the shared task engine as we are getting issues with 
         * serialising Apache Log4J Loggers
         * taskEngine(new HazelcastTaskEngine());
         */
        // session engine
        sessionEngine(new HazelcastSessionEngine());
        // security engine
        securityEngine(new BergamotSecurityEngine());
        // setup the application security key
        if (! Util.isEmpty(this.getConfiguration().getSecurityKey()))
        {
            // set the key
            this.getSecurityEngine().applicationKey(SecretKey.fromString(this.getConfiguration().getSecurityKey()));
        }
        // setup accounting
        AccountingManager.getInstance().registerConsumer("logger", new BergamotLoggingConsumer());
        AccountingManager.getInstance().registerConsumer("queue", new BergamotAccountingQueueConsumer());
        AccountingManager.getInstance().bindRootConsumer("logger");
        AccountingManager.getInstance().bindRootConsumer("queue");
        // setup ClusterManager to manage our critical
        // resources across the cluster
        this.clusterManager = new ClusterManager();
        // websocket update server
        this.updateServer = new UpdateServer(this.config.getListen().getWebsocketPort());
    }

    @Override
    protected void setupFunctions() throws Exception
    {
        // express functions
        immutableFunction(new BergamotUpdateURL());
        immutableFunction(new BergamotJSVersion());
        immutableFunction(new BergamotCSSVersion());
    }
    
    @Override
    protected void setupActions() throws Exception
    {
        // some actions
        action(new ExecuteCheckAction());
        action(new SchedulerActions());
        action(new DispatchResultAction());
        action(new TimePeriodActions());
        action(new TeamActions());
        action(new ContactActions());
        action(new ConfigChangeActions());
        action(new CheckActions());
        action(new BergamotAgentActions());
        action(new U2FAActions());
    }
    
    @Override
    protected void setupRouters() throws Exception
    {
        // UI filters and defaults
        router(new UIRouter());
        // health check router
        router(new HealthRouter());
        // Setup the application routers
        router(new ErrorRouter());
        router(new LoginRouter());
        router(new DashboardRouter());
        router(new GroupsRouter());
        router(new ServiceRouter());
        router(new HostRouter());
        router(new LocationRouter());
        router(new TrapRouter());
        router(new ClusterRouter());
        router(new ResourceRouter());
        router(new TeamRouter());
        router(new ContactRouter());
        router(new TimePeriodRouter());
        router(new ProfileRouter());
        router(new StatsRouter());
        router(new CommandRouter());
        router(new AlertsRouter());
        router(new StatusRouter());
        router(new SLARouter());
        // Agent
        router(new AgentRouter());
        // About
        router(new AboutRouter());
        // Admin
        router(new AdminRouter());
        router(new ContactAdminRouter());
        router(new TeamAdminRouter());
        router(new TimePeriodAdminRouter());
        router(new CommandAdminRouter());
        router(new LocationAdminRouter());
        router(new GroupAdminRouter());
        router(new HostAdminRouter());
        router(new ServiceAdminRouter());
        router(new TrapAdminRouter());
        router(new ClusterAdminRouter());
        router(new ResourceAdminRouter());
        router(new ConfigChangeAdminRouter());
        router(new ConfigAdminRouter());
        router(new SecurityDomainAdminRouter());
        router(new CredentialAdminRouter());
        // Command Editor
        router(new CommandEditorRouter());
        // Global Stuff
        router(new FirstInstallRouter());
        router(new GlobalAdminRouter());
        router(new GlobalUtilsAdminRouter());
        // API
        router(new APIRouter());
        router(new MetricsAPIRouter());
        router(new AlertsAPIRouter());
        router(new HostAPIRouter());
        router(new LocationAPIRouter());
        router(new GroupAPIRouter());
        router(new ClusterAPIRouter());
        router(new ServiceAPIRouter());
        router(new TrapAPIRouter());
        router(new ResourceAPIRouter());
        router(new TimePeriodAPIRouter());
        router(new CommandAPIRouter());
        router(new ContactAPIRouter());
        router(new TeamAPIRouter());
        router(new TestAPIRouter());
        router(new CommentsAPIRouter());
        router(new DowntimeAPIRouter());
        router(new ConfigAPIRouter());
        router(new StatsAPIRouter());
        router(new UtilAPIRouter());
        router(new AgentAPIRouter());
        router(new LamplighterAPIRouter());
    }
    
    @Override
    protected void startApplication() throws Exception
    {
        // setup healthcheck tracker
        HealthTracker.getInstance().init();
        // setup healthcheck agent
        HealthAgent.getInstance().init("ui", "bergamot-ui");
        // setup the database
        BergamotDB.install();
        try (BergamotDB db = BergamotDB.connect())
        {
            System.out.println("Database module: " + db.getName() + " " + db.getVersion());
        }
        LamplighterDB.install();
        try (LamplighterDB db = LamplighterDB.connect())
        {
            System.out.println("Database module: " + db.getName() + " " + db.getVersion());
        }
        // don't bother starting scheduler etc on ui only nodes
        if (!Boolean.getBoolean("bergamot.ui.only"))
        {
            // start the cluster manager
            this.clusterManager.start(this.getInstanceName());
            // register sites with the cluster manager
            try (BergamotDB db = BergamotDB.connect())
            {
                for (Site site : db.listSites())
                {
                    if (! site.isDisabled())
                        this.clusterManager.registerSite(site);
                }
            }
        }
        // start the update websocket server
        this.updateServer.start();
        // Start Gerald
        // Gerald.theMole().start();
        System.out.println("Application startup finished");
    }
    
    public ClusterManager getClusterManager()
    {
        return this.clusterManager;
    }

    public UpdateServer getUpdateServer()
    {
        return updateServer;
    }

    public static void main(String[] args) throws Exception
    {
        try
        {
            // read config
            UICfg config = UICfg.loadConfiguration();
            System.out.println("Using configuration: ");
            System.out.println(config.toString());
            // create the application
            BergamotApp bergamotApp = new BergamotApp();
            bergamotApp.configure(config);
            // Setup logging ASAP
            bergamotApp.configureLogging();
            // compile database
            System.out.println("Compiling DB");
            BergamotDB.load();
            LamplighterDB.load();
            // setup the cache
            System.out.println("Setting up Hazelcast");
            DataManager.get().registerCacheProvider("hazelcast", new HazelcastCacheProvider(BergamotApp.getApplicationInstanceName()));
            DataManager.get().registerDefaultCacheProvider(DataManager.get().cacheProvider("hazelcast"));
            // setup the queue manager
            System.out.println("Setting up RabbitMQ");
            QueueUtil.setupQueueBroker(config.getBroker(), "bergamot-ui");
            // setup data manager
            System.out.println("Setting up PostgreSQL");
            DataManager.getInstance().registerDefaultServer(DatabasePool.Default.with().postgresql().url(config.getDatabase().getUrl()).username(config.getDatabase().getUsername()).password(config.getDatabase().getPassword()).build());
            // start the app
            System.out.println("Starting Bergamot UI");
            bergamotApp.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
    public static String getApplicationInstanceName()
    {
        return BalsaApplication.getApplicationInstanceName(BergamotApp.class);
    }
}
