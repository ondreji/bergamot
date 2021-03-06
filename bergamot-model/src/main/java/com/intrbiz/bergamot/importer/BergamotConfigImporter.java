package com.intrbiz.bergamot.importer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.codahale.metrics.Timer;
import com.intrbiz.Util;
import com.intrbiz.bergamot.config.model.AccessControlCfg;
import com.intrbiz.bergamot.config.model.ActiveCheckCfg;
import com.intrbiz.bergamot.config.model.BergamotCfg;
import com.intrbiz.bergamot.config.model.CheckCfg;
import com.intrbiz.bergamot.config.model.ClusterCfg;
import com.intrbiz.bergamot.config.model.CommandCfg;
import com.intrbiz.bergamot.config.model.ContactCfg;
import com.intrbiz.bergamot.config.model.CredentialCfg;
import com.intrbiz.bergamot.config.model.EscalateCfg;
import com.intrbiz.bergamot.config.model.GroupCfg;
import com.intrbiz.bergamot.config.model.HostCfg;
import com.intrbiz.bergamot.config.model.LocationCfg;
import com.intrbiz.bergamot.config.model.NamedObjectCfg;
import com.intrbiz.bergamot.config.model.NotificationEngineCfg;
import com.intrbiz.bergamot.config.model.NotificationsCfg;
import com.intrbiz.bergamot.config.model.PassiveCheckCfg;
import com.intrbiz.bergamot.config.model.RealCheckCfg;
import com.intrbiz.bergamot.config.model.ResourceCfg;
import com.intrbiz.bergamot.config.model.SLACfg;
import com.intrbiz.bergamot.config.model.SLAFixedPeriodCfg;
import com.intrbiz.bergamot.config.model.SLAPeriodCfg;
import com.intrbiz.bergamot.config.model.SLARollingPeriodCfg;
import com.intrbiz.bergamot.config.model.SecuredObjectCfg;
import com.intrbiz.bergamot.config.model.SecurityDomainCfg;
import com.intrbiz.bergamot.config.model.ServiceCfg;
import com.intrbiz.bergamot.config.model.TeamCfg;
import com.intrbiz.bergamot.config.model.TemplatedObjectCfg;
import com.intrbiz.bergamot.config.model.TemplatedObjectCfg.ObjectState;
import com.intrbiz.bergamot.config.model.TimePeriodCfg;
import com.intrbiz.bergamot.config.model.TrapCfg;
import com.intrbiz.bergamot.config.model.UpdateNotificationsCfg;
import com.intrbiz.bergamot.config.model.VirtualCheckCfg;
import com.intrbiz.bergamot.config.validator.ValidatedBergamotConfiguration;
import com.intrbiz.bergamot.data.BergamotDB;
import com.intrbiz.bergamot.model.AccessControl;
import com.intrbiz.bergamot.model.ActiveCheck;
import com.intrbiz.bergamot.model.Check;
import com.intrbiz.bergamot.model.CheckCommand;
import com.intrbiz.bergamot.model.Cluster;
import com.intrbiz.bergamot.model.Command;
import com.intrbiz.bergamot.model.Config;
import com.intrbiz.bergamot.model.Contact;
import com.intrbiz.bergamot.model.Credential;
import com.intrbiz.bergamot.model.Escalation;
import com.intrbiz.bergamot.model.Group;
import com.intrbiz.bergamot.model.Host;
import com.intrbiz.bergamot.model.Location;
import com.intrbiz.bergamot.model.NotificationEngine;
import com.intrbiz.bergamot.model.Notifications;
import com.intrbiz.bergamot.model.PassiveCheck;
import com.intrbiz.bergamot.model.RealCheck;
import com.intrbiz.bergamot.model.Resource;
import com.intrbiz.bergamot.model.SLA;
import com.intrbiz.bergamot.model.SLAFixedPeriod;
import com.intrbiz.bergamot.model.SLARollingPeriod;
import com.intrbiz.bergamot.model.SecuredObject;
import com.intrbiz.bergamot.model.SecurityDomain;
import com.intrbiz.bergamot.model.Service;
import com.intrbiz.bergamot.model.Site;
import com.intrbiz.bergamot.model.Status;
import com.intrbiz.bergamot.model.Team;
import com.intrbiz.bergamot.model.TimePeriod;
import com.intrbiz.bergamot.model.Trap;
import com.intrbiz.bergamot.model.VirtualCheck;
import com.intrbiz.bergamot.model.message.notification.Notification;
import com.intrbiz.bergamot.model.message.notification.RegisterContactNotification;
import com.intrbiz.bergamot.model.message.scheduler.EnableCheck;
import com.intrbiz.bergamot.model.message.scheduler.RescheduleCheck;
import com.intrbiz.bergamot.model.message.scheduler.ScheduleCheck;
import com.intrbiz.bergamot.model.message.scheduler.SchedulerAction;
import com.intrbiz.bergamot.model.message.scheduler.UnscheduleCheck;
import com.intrbiz.bergamot.model.state.CheckState;
import com.intrbiz.bergamot.model.state.CheckStats;
import com.intrbiz.bergamot.queue.NotificationQueue;
import com.intrbiz.bergamot.queue.SchedulerQueue;
import com.intrbiz.bergamot.queue.key.NotificationKey;
import com.intrbiz.bergamot.queue.key.SchedulerKey;
import com.intrbiz.bergamot.virtual.VirtualCheckExpressionContext;
import com.intrbiz.bergamot.virtual.VirtualCheckExpressionParser;
import com.intrbiz.bergamot.virtual.operator.VirtualCheckOperator;
import com.intrbiz.bergamot.virtual.reference.CheckReference;
import com.intrbiz.configuration.CfgParameter;
import com.intrbiz.configuration.Configuration;
import com.intrbiz.data.DataException;
import com.intrbiz.gerald.witchcraft.Witchcraft;
import com.intrbiz.queue.RoutedProducer;

public class BergamotConfigImporter
{    
    private BergamotImportReport  report;
    
    private Site site;
    
    private BergamotCfg config;
    
    private boolean resetState = false;
    
    private boolean createSite = false;
    
    private boolean online = false;
    
    private Map<String, CascadedChange> cascadedChanges = new HashMap<String, CascadedChange>();
    
    private Set<String> loadedObjects = new HashSet<String>();
    
    private List<DelayedSchedulerAction> delayedSchedulerActions = new LinkedList<DelayedSchedulerAction>();
    
    private List<Contact> delayedContactRegistrations = new LinkedList<Contact>();
    
    private Function<Contact, String> registrationURLSupplier;
    
    private boolean rebuildPermissions = false;
    
    private boolean clearPermissionsCache = false;
    
    private Timer importTimer = Witchcraft.get().source("com.intrbiz.config.bergamot").getRegistry().timer(Witchcraft.name(BergamotConfigImporter.class, "import_time"));
    
    private String defaultPassword = "bergamot";
    
    private boolean requirePasswordChange = true;
    
    private Set<String> updatedResourcePools = new HashSet<String>();
    
    public BergamotConfigImporter(ValidatedBergamotConfiguration validated)
    {
        if (! validated.getReport().isValid()) throw new RuntimeException("Cannot import invalid configuration");
        this.config = validated.getConfig();
    }
    
    public BergamotConfigImporter defaultPassword(String password)
    {
        this.defaultPassword = password;
        return this;
    }
    
    public BergamotConfigImporter requirePasswordChange(boolean change)
    {
        this.requirePasswordChange = change;
        return this;
    }
    
    public BergamotConfigImporter resetState(boolean resetState)
    {
        this.resetState = resetState;
        return this;
    }
    
    public BergamotConfigImporter createSite(boolean createSite)
    {
        this.createSite = createSite;
        return this;
    }
    
    public BergamotConfigImporter online(boolean online)
    {
        this.online = online;
        return this;
    }
    
    public BergamotConfigImporter online()
    {
        this.online = true;
        return this;
    }
    
    public BergamotConfigImporter offline()
    {
        this.online = false;
        return this;
    }
    
    public BergamotConfigImporter registrationURLSupplier(Function<Contact, String> registrationURLSupplier)
    {
        this.registrationURLSupplier = registrationURLSupplier;
        return this;
    }
    
    public BergamotImportReport importConfiguration()
    {
        if (this.report == null)
        {
            try (Timer.Context tctx = this.importTimer.time())
            {
                this.report = new BergamotImportReport(this.config.getSite());
                try
                {
                    // update database
                    try (BergamotDB db = BergamotDB.connect())
                    {
                        db.execute(()-> {
                            // setup the site
                            this.loadSite(db);
                            // compute any cascading changes
                            this.computeCascade(db);
                            // load any security domains
                            this.loadSecurityDomains(db);
                            // templates
                            this.loadTemplates(db);
                            // load the credentials
                            this.loadCredentials(db);
                            // load the commands
                            this.loadCommands(db);
                            // time periods
                            this.loadTimePeriods(db);
                            // teams
                            this.loadTeams(db);
                            // contacts
                            this.loadContacts(db);
                            // load the locations
                            this.loadLocations(db);
                            // groups
                            this.loadGroups(db);
                            // hosts
                            this.loadHosts(db);
                            // clusters
                            this.loadClusters(db);
                            // link any check to check dependencies
                            this.linkDependencies(db);
                            // update any virtual checks
                            this.updateResourcePools(db);
                            // rebuild computed permissions
                            if (this.rebuildPermissions)
                            {
                                this.report.info("Rebuilding computed permissions");
                                db.buildPermissions(this.site.getId());
                            }
                            if (this.clearPermissionsCache)
                            {
                                this.report.info("Clearing permissions cache");
                                db.invalidatePermissionsCache(this.site.getId());
                            }
                        });
                        // delayed actions
                        if (this.online)
                        {
                            // we must publish any scheduling changes after we have committed the transaction
                            // publish all scheduling changes
                            try (SchedulerQueue queue = SchedulerQueue.open())
                            {
                                try (RoutedProducer<SchedulerAction, SchedulerKey> producer = queue.publishSchedulerActions())
                                {
                                    for (DelayedSchedulerAction delayedAction : this.delayedSchedulerActions)
                                    {
                                        producer.publish(delayedAction.key, delayedAction.action);
                                    }
                                }
                            }
                            // we must publish and contact registration notifications
                            try (NotificationQueue notificationQueue = NotificationQueue.open())
                            {
                                try (RoutedProducer<Notification, NotificationKey> notificationsProducer = notificationQueue.publishNotifications())
                                {
                                    for (Contact contact : this.delayedContactRegistrations)
                                    {
                                        try
                                        {
                                            // get the registration url
                                            String url = this.registrationURLSupplier.apply(contact);
                                            // send a notification, only via email
                                            notificationsProducer.publish(
                                                    new NotificationKey(contact.getSite().getId()),
                                                    new RegisterContactNotification(contact.getSite().toMOUnsafe(), contact.toMOUnsafe().addEngine("email"), url) 
                                            );
                                        }
                                        catch (Exception e)
                                        {
                                            Logger.getLogger(BergamotConfigImporter.class).error("Failed to send registration notification", e);
                                            throw e;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                catch (Throwable e)
                {
                    Logger.getLogger(BergamotConfigImporter.class).error("Failed to import configuration", e);
                    this.report.error("Configuration change aborted due to unhandled error: " + e.getMessage());
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    this.report.error(sw.toString());
                }
            }
        }
        return this.report;
    }
    
    private void loadSite(BergamotDB db)
    {
        this.site = db.getSiteByName(this.config.getSite());
        if (this.site == null)
        {
            if (this.createSite)
            {
                this.site = new Site();
                this.site.setId(Site.randomSiteId());
                this.site.setName(this.config.getSite());
                this.site.setSummary(this.config.getSite());
                db.setSite(site);
            }
            else
            {
                this.report.error("Site '" + this.config.getSite() + "' does not exist and cannot be created, aborting!");
                throw new RuntimeException("Site '" + this.config.getSite() + "' does not exist and cannot be created, aborting!");
            }
        }
        // update any parameters
        if (! this.config.getParameters().isEmpty())
        {
            for (CfgParameter parameter : this.config.getParameters())
            {
                this.site.setParameter(parameter.getName(), parameter.getValueOrText());
            }
            db.setSite(site);
        }
    }
    
    private void loadTemplates(BergamotDB db)
    {
        for (List<? extends TemplatedObjectCfg<?>> objects : this.config.getAllObjects())
        {
            for (TemplatedObjectCfg<?> object : objects)
            {
                if (object.getTemplateBooleanValue() && object instanceof NamedObjectCfg)
                {
                    String type = Configuration.getRootElement(((NamedObjectCfg<?>) object).getClass());
                    Config conf = db.getConfigByName(this.site.getId(), type, object.getName());
                    if (conf == null)
                    {
                        conf =  new Config(this.site.randomObjectId(), this.site.getId(), (NamedObjectCfg<?>) object);
                        this.report.info("Configuring new " + type + " template: " + object.getName());
                        db.setConfig(conf);
                        // add the template to the correct security domains
                        if (object instanceof SecuredObjectCfg)
                        {
                            this.linkSecurityDomains((SecuredObjectCfg<?>) object.resolve(), conf.getId(), db);
                        }
                    }
                    else
                    {
                        if (ObjectState.isRemove(object.getObjectState()))
                        {
                            if (conf.listAllDependentsObjects().isEmpty())
                            {
                                // nothing is using this template, so remove it
                                this.report.info("Removing existing " + type + " template: " + object.getName() + " (" + conf.getId() + ")");
                                db.removeConfig(conf.getId());
                                db.removeSecurityDomainMembershipForCheck(conf.getId());
                            }
                            else
                            {
                                // we cannot remove this template, it is in use
                                throw new RuntimeException("The " + type + " template " + conf.getName() + " cannot be removed as it is in use.");
                            }
                        }
                        else
                        {
                            conf.fromConfiguration((NamedObjectCfg<?>) object);
                            this.report.info("Reconfiguring existing " + type + " template: " + object.getName() + " (" + conf.getId() + ")");
                            db.setConfig(conf);
                        }
                    }
                }
            }
        }
    }
    
    private void computeCascade(BergamotDB db)
    {
        for (List<? extends TemplatedObjectCfg<?>> objects : this.config.getAllObjects())
        {
            for (TemplatedObjectCfg<?> object : objects)
            {
                String type = Configuration.getRootElement(((NamedObjectCfg<?>) object).getClass());
                Config conf = db.getConfigByName(this.site.getId(), type, object.getName());
                if (conf != null)
                {
                    List<Config> dependents = conf.listAllDependentsObjects();
                    if (ObjectState.isRemove(object.getObjectState()))
                    {
                        if (! dependents.isEmpty())
                        {
                            // currently we restrict cascading removal
                            throw new RuntimeException("The " + type + " " + conf.getName() + " cannot be removed as it is in use by: " + dependents.stream().map((c) -> c.getType() + " " + c.getName()).collect(Collectors.joining(", ")));
                        }
                    }
                    else
                    {
                        this.report.info("  Change to " + type + ":" + conf.getName());
                        // cascade - note this recursively queries for all objects affected by a change to this template
                        for (Config config : dependents)
                        {
                            this.report.info("  cascades to: " + config.getType() + ":" + config.getName());
                            this.cascadedChanges.put(config.getQualifiedName(), new CascadedChange(object, conf, config));
                        }
                    }
                }
            }
        }
    }
    
    private void loadSecurityDomains(BergamotDB db)
    {
        for (SecurityDomainCfg configuration : this.config.getSecurityDomains())
        {
            if (! configuration.getTemplateBooleanValue())
            {
                if (ObjectState.isRemove(configuration.getObjectState()))
                {
                    this.removeSecurityDomain(configuration, db);
                }
                else
                {
                    this.loadSecurityDomain(configuration, db);
                }
            }
        }
    }
    
    private void removeSecurityDomain(SecurityDomainCfg configuration, BergamotDB db)
    {
        this.report.info("Removing security domain: " + configuration.resolve().getName());
        SecurityDomain securityDomain = db.getSecurityDomainByName(this.site.getId(), configuration.getName());
        if (securityDomain != null)
        {
            db.removeSecurityDomain(securityDomain.getId());
            db.removeConfig(securityDomain.getId());
        }
    }
    
    private void loadSecurityDomain(SecurityDomainCfg configuration, BergamotDB db)
    {
        if (this.loadedObjects.contains("security_domain:" + configuration.getName()))
        {
            this.report.info("Skipping reconfiguring security domain " + configuration.getName());
            return;
        }
        // load
        SecurityDomain securityDomain = db.getSecurityDomainByName(this.site.getId(), configuration.getName());
        if(securityDomain == null)
        {
            configuration.setId(this.site.randomObjectId());
            securityDomain = new SecurityDomain();
            this.report.info("Configuring new security domain: " + configuration.resolve().getName());
        }
        else
        {
            configuration.setId(securityDomain.getId());
            this.report.info("Reconfiguring existing security domain: " + configuration.resolve().getName() + " (" + configuration.getId() + ")");
        }
        // apply the new config
        securityDomain.configure(configuration);
        // update
        db.setSecurityDomain(securityDomain);
        this.loadedObjects.add("security_domain:" + configuration.getName());
    }
    
    private void loadCommands(BergamotDB db)
    {
        for (CommandCfg configuration : this.config.getCommands())
        {
            if (! configuration.getTemplateBooleanValue())
            {
                if (ObjectState.isRemove(configuration.getObjectState()))
                {
                    // remove the command
                    this.removeCommand(configuration, db);
                }
                else
                {
                    // add or change the command
                    this.loadCommand(configuration, db);
                }
            }
        }
        // load any commands where a template change cascades
        for (CascadedChange change : this.cascadedChanges.values())
        {
            if (change.dependent.getConfiguration() instanceof CommandCfg)
            {
                CommandCfg configuration = (CommandCfg) change.dependent.getConfiguration();
                if (! (configuration.getTemplateBooleanValue() || this.loadedObjects.contains("command:" + configuration.getName())))
                {
                    this.report.info("Reconfiguring command " + configuration.getName() + " due to a change to the " + change.template.getName() + " inherited template.");
                    // first we need to resolve the inheritance for the cascaded object
                    db.getConfigResolver(this.site.getId()).computeInheritenance(configuration);
                    // load
                    this.loadCommand(configuration, db);
                }
            }
        }
    }
    
    private void removeCommand(CommandCfg configuration, BergamotDB db)
    {
        this.report.info("Removing command: " + configuration.resolve().getName());
        // remove the command
        Command command = db.getCommandByName(this.site.getId(), configuration.getName());
        if (command != null)
        {
            db.removeCommand(command.getId());
            db.removeConfig(command.getId());
            db.removeSecurityDomainMembershipForCheck(command.getId());
        }
    }
    
    private void loadCommand(CommandCfg configuration, BergamotDB db)
    {
        if (this.loadedObjects.contains("command:" + configuration.getName()))
        {
            this.report.info("Skipping reconfiguring command " + configuration.getName());
            return;
        }
        // load
        CommandCfg resolvedConfiguration = configuration.resolve();
        Command command = db.getCommandByName(this.site.getId(), configuration.getName());
        if(command == null)
        {
            configuration.setId(this.site.randomObjectId());
            command = new Command();
            this.report.info("Configuring new command: " + resolvedConfiguration.getName());
        }
        else
        {
            configuration.setId(command.getId());
            this.report.info("Reconfiguring existing command: " + resolvedConfiguration.getName() + " (" + configuration.getId() + ")");
        }
        // apply the new config
        command.configure(configuration);
        // update
        db.setCommand(command);
        this.loadedObjects.add("command:" + configuration.getName());
        // security domains
        this.linkSecurityDomains(resolvedConfiguration, command, db);
    }

    private void loadLocations(BergamotDB db)
    {
        for (LocationCfg configuration : this.config.getLocations())
        {
            if (! configuration.getTemplateBooleanValue())
            {
                if (ObjectState.isRemove(configuration.getObjectState()))
                {
                    // remove the location
                    this.removeLocation(configuration, db);
                }
                else
                {
                    // add or change the location
                    this.loadLocation(configuration, db);
                }
            }
        }
        // link the tree
        for (LocationCfg configuration : this.config.getLocations())
        {
            if (! configuration.getTemplateBooleanValue())
            {
                if (ObjectState.isChange(configuration.getObjectState()))
                {
                    // only link locations that are being changed or added
                    this.linkLocation(configuration, db);
                }
            }
        }
        // load any location where a template change cascades
        for (CascadedChange change : this.cascadedChanges.values())
        {
            if (change.dependent.getConfiguration() instanceof LocationCfg)
            {
                LocationCfg configuration = (LocationCfg) change.dependent.getConfiguration();
                if (! (configuration.getTemplateBooleanValue() || this.loadedObjects.contains("location:" + configuration.getName())))
                {
                    this.report.info("Reconfiguring location " + configuration.getName() + " due to a change to the " + change.template.getName() + " inherited template.");
                    // first we need to resolve the inheritance for the cascaded object
                    db.getConfigResolver(this.site.getId()).computeInheritenance(configuration);
                    // load
                    this.loadLocation(configuration, db);
                    this.linkLocation(configuration, db);
                }
            }
        }
    }
    
    private void removeLocation(LocationCfg configuration, BergamotDB db)
    {
        this.report.info("Removing location: " + configuration.resolve().getName());
        // remove the location
        Location location = db.getLocationByName(this.site.getId(), configuration.getName());
        if (location != null)
        {
            db.removeLocation(location.getId());
            db.removeConfig(location.getId());
            db.removeSecurityDomainMembershipForCheck(location.getId());
        }
    }
    
    private void loadLocation(LocationCfg configuration, BergamotDB db)
    {
        if (this.loadedObjects.contains("location:" + configuration.getName()))
        {
            this.report.info("Skipping reconfiguring location " + configuration.getName());
            return;
        }
        // load
        LocationCfg resolvedConfiguration = configuration.resolve();
        Location location = db.getLocationByName(this.site.getId(), configuration.getName());
        if (location == null)
        {
            configuration.setId(this.site.randomObjectId());
            location = new Location();
            this.report.info("Configuring new location: " + resolvedConfiguration.getName());
        }
        else
        {
            configuration.setId(location.getId());
            this.report.info("Reconfiguring existing location: " + resolvedConfiguration.getName() + " (" + configuration.getId() + ")");
        }
        location.configure(configuration);
        db.setLocation(location);
        this.loadedObjects.add("location:" + configuration.getName());
        // security domains
        this.linkSecurityDomains(resolvedConfiguration, location, db);
    }
    
    private void linkLocation(LocationCfg configuration, BergamotDB db)
    {
        Location l = db.getLocation(configuration.getId());
        if (l != null)
        {
            String pn = configuration.resolve().getLocation();
            if (!Util.isEmpty(pn))
            {
                Location p = db.getLocationByName(this.site.getId(), pn);
                if (p != null)
                {
                    this.report.info("Adding location " + l.getName() + " to location " + p.getName());
                    db.addLocationChild(p, l);
                }
            }
        }
    }
    
    private void loadGroups(BergamotDB db)
    {
        for (GroupCfg configuration : this.config.getGroups())
        {
            if (! configuration.getTemplateBooleanValue())
            {
                if (ObjectState.isRemove(configuration.getObjectState()))
                {
                    this.removeGroup(configuration, db);
                }
                else
                {
                    this.loadGroup(configuration, db);
                }
            }
        }
        // link the tree
        for (GroupCfg configuration : this.config.getGroups())
        {
            if (! configuration.getTemplateBooleanValue())
            {
                if (ObjectState.isChange(configuration.getObjectState()))
                {
                    this.linkGroup(configuration, db);
                }
            }
        }
        // load any group where a template change cascades
        for (CascadedChange change : this.cascadedChanges.values())
        {
            if (change.dependent.getConfiguration() instanceof GroupCfg)
            {
                GroupCfg configuration = (GroupCfg) change.dependent.getConfiguration();
                if (! (configuration.getTemplateBooleanValue() || this.loadedObjects.contains("group:" + configuration.getName())))
                {
                    this.report.info("Reconfiguring group " + configuration.getName() + " due to a change to the " + change.template.getName() + " inherited template.");
                    // first we need to resolve the inheritance for the cascaded object
                    db.getConfigResolver(this.site.getId()).computeInheritenance(configuration);
                    // load
                    this.loadGroup(configuration, db);
                    this.linkGroup(configuration, db);
                }
            }
        }
    }
    
    private void removeGroup(GroupCfg configuration, BergamotDB db)
    {
        this.report.info("Removing group: " + configuration.resolve().getName());
        // remove the group
        Group group = db.getGroupByName(this.site.getId(), configuration.getName());
        if (group != null)
        {
            db.removeGroup(group.getId());
            db.removeConfig(group.getId());
            db.removeSecurityDomainMembershipForCheck(group.getId());
        }
    }
    
    private void loadGroup(GroupCfg configuration, BergamotDB db)
    {
        if (this.loadedObjects.contains("group:" + configuration.getName()))
        {
            this.report.info("Skipping reconfiguring group " + configuration.getName());
            return;
        }
        GroupCfg resolvedConfiguration = configuration.resolve();
        // load
        Group group = db.getGroupByName(this.site.getId(), configuration.getName());
        if (group == null)
        {
            configuration.setId(this.site.randomObjectId());
            group = new Group();
            this.report.info("Configuring new group: " + resolvedConfiguration.getName());
        }
        else
        {
            configuration.setId(group.getId());
            this.report.info("Reconfiguring existing group: " + resolvedConfiguration.getName() + " (" + configuration.getId() + ")");
        }
        group.configure(configuration);
        db.setGroup(group);
        this.loadedObjects.add("group:" + configuration.getName());
        // security domains
        this.linkSecurityDomains(resolvedConfiguration, group, db);
    }
    
    private void linkGroup(GroupCfg configuration, BergamotDB db)
    {
        Group child = db.getGroup(configuration.getId());
        if (child != null)
        {
            for (String parentName : configuration.resolve().getGroups())
            {
                Group parent = db.getGroupByName(this.site.getId(), parentName);
                if (parent != null)
                {
                    this.report.info("Adding group " + child.getName() + " to group " + parent.getName());
                    db.addGroupChild(parent, child);
                }
            }
        }
    }
    
    private void loadTimePeriods(BergamotDB db)
    {
        for (TimePeriodCfg configuration : this.config.getTimePeriods())
        {
            if (! configuration.getTemplateBooleanValue())
            {
                if (ObjectState.isRemove(configuration.getObjectState()))
                {
                    this.removeTimePeriod(configuration, db);
                }
                else
                {
                    this.loadTimePeriod(configuration, db);
                }
            }
        }
        // link excludes
        for (TimePeriodCfg configuration : this.config.getTimePeriods())
        {
            if (! configuration.getTemplateBooleanValue())
            {
                if (ObjectState.isChange(configuration.getObjectState()))
                {
                    this.linkTimePeriod(configuration, db);
                }
            }
        }
        // load any timeperiod where a template change cascades
        for (CascadedChange change : this.cascadedChanges.values())
        {
            if (change.dependent.getConfiguration() instanceof TimePeriodCfg)
            {
                TimePeriodCfg configuration = (TimePeriodCfg) change.dependent.getConfiguration();
                if (! (configuration.getTemplateBooleanValue() || this.loadedObjects.contains("timeperiod:" + configuration.getName())))
                {
                    this.report.info("Reconfiguring time period " + configuration.getName() + " due to a change to the " + change.template.getName() + " inherited template.");
                    // first we need to resolve the inheritance for the cascaded object
                    db.getConfigResolver(this.site.getId()).computeInheritenance(configuration);
                    // load
                    this.loadTimePeriod(configuration, db);
                    this.linkTimePeriod(configuration, db);
                }
            }
        }
    }
    
    private void removeTimePeriod(TimePeriodCfg configuration, BergamotDB db)
    {
        this.report.info("Removing timeperiod: " + configuration.resolve().getName());
        TimePeriod timePeriod = db.getTimePeriodByName(this.site.getId(), configuration.getName());
        if (timePeriod != null)
        {
            db.removeTimePeriod(timePeriod.getId());
            db.removeConfig(timePeriod.getId());
            db.removeSecurityDomainMembershipForCheck(timePeriod.getId());
        }
    }
    
    private void loadTimePeriod(TimePeriodCfg configuration, BergamotDB db)
    {
        if (this.loadedObjects.contains("timeperiod:" + configuration.getName()))
        {
            this.report.info("Skipping reconfiguring timeperiod " + configuration.getName());
            return;
        }
        // load
        TimePeriodCfg resolvedConfiguration = configuration.resolve();
        TimePeriod timePeriod = db.getTimePeriodByName(this.site.getId(), configuration.getName());
        if (timePeriod == null)
        {
            configuration.setId(this.site.randomObjectId());
            timePeriod = new TimePeriod();
            this.report.info("Configuring new timeperiod: " + resolvedConfiguration.getName());
        }
        else
        {
            configuration.setId(timePeriod.getId());
            this.report.info("Reconfiguring existing timeperiod: " + resolvedConfiguration.getName() + " (" + configuration.getId() + ")");
        }
        timePeriod.configure(configuration);
        db.setTimePeriod(timePeriod);
        this.loadedObjects.add("timeperiod:" + configuration.getName());
        // security domains
        this.linkSecurityDomains(resolvedConfiguration, timePeriod, db);
    }
    
    private void linkTimePeriod(TimePeriodCfg configuration, BergamotDB db)
    {
        TimePeriod timePeriod = db.getTimePeriod(configuration.getId());
        if (timePeriod != null)
        {
            for (String excludeName : configuration.resolve().getExcludes())
            {
                TimePeriod excluded = db.getTimePeriodByName(this.site.getId(), excludeName);
                if (excluded != null)
                {
                    this.report.info("Adding excluded timeperiod " + excluded.getName() + " to timeperiod " + timePeriod.getName());
                    db.addTimePeriodExclude(timePeriod, excluded);
                }
            }
        }
    }

    private void loadTeams(BergamotDB db)
    {
        for (TeamCfg configuration : this.config.getTeams())
        {
            if (! configuration.getTemplateBooleanValue())
            {
                if (ObjectState.isRemove(configuration.getObjectState()))
                {
                    this.removeTeam(configuration, db);
                }
                else
                {
                    this.loadTeam(configuration, db);
                }
            }
        }
        // link the tree
        for (TeamCfg configuration : this.config.getTeams())
        {
            if (! configuration.getTemplateBooleanValue())
            {
                if (ObjectState.isChange(configuration.getObjectState()))
                {
                    this.linkTeam(configuration, db);
                }
            }
        }
        // load any team where a template change cascades
        for (CascadedChange change : this.cascadedChanges.values())
        {
            if (change.dependent.getConfiguration() instanceof TeamCfg)
            {
                TeamCfg configuration = (TeamCfg) change.dependent.getConfiguration();
                if (! (configuration.getTemplateBooleanValue() || this.loadedObjects.contains("team:" + configuration.getName())))
                {
                    this.report.info("Reconfiguring team " + configuration.getName() + " due to a change to the " + change.template.getName() + " inherited template.");
                    // first we need to resolve the inheritance for the cascaded object
                    db.getConfigResolver(this.site.getId()).computeInheritenance(configuration);
                    // load
                    this.loadTeam(configuration, db);
                    this.linkTeam(configuration, db);
                }
            }
        }
    }
    
    private void removeTeam(TeamCfg configuration, BergamotDB db)
    {
        this.report.info("Removing team: " + configuration.resolve().getName());
        Team team = db.getTeamByName(this.site.getId(), configuration.getName());
        if (team != null)
        {
            db.removeTeam(team.getId());
            db.removeConfig(team.getId());
            db.removeSecurityDomainMembershipForCheck(team.getId());
        }
    }
    
    private void loadTeam(TeamCfg configuration, BergamotDB db)
    {
        if (this.loadedObjects.contains("team:" + configuration.getName()))
        {
            this.report.info("Skipping reconfiguring team " + configuration.getName());
            return;
        }
        // load
        TeamCfg resolvedConfiguration = configuration.resolve();
        Team team = db.getTeamByName(this.site.getId(), configuration.getName());
        if (team == null)
        {
            configuration.setId(this.site.randomObjectId());
            team = new Team();
            this.report.info("Configuring new team: " + resolvedConfiguration.getName());
        }
        else
        {
            configuration.setId(team.getId());
            this.report.info("Reconfiguring existing team: " + resolvedConfiguration.getName() + " (" + configuration.getId() + ")");
        }
        team.configure(configuration);
        // access controls
        this.loadAccessControls(team.getId(), configuration.resolve().getAccessControls(), db);
        db.setTeam(team);
        this.loadedObjects.add("team:" + configuration.getName());
        // we need to rebuild the permissions
        this.rebuildPermissions = true;
        // security domains
        this.linkSecurityDomains(resolvedConfiguration, team, db);
    }
    
    private void linkTeam(TeamCfg configuration, BergamotDB db)
    {
        Team child = db.getTeam(configuration.getId());
        if (child != null)
        {
            for (String parentName : configuration.resolve().getTeams())
            {
                Team parent = db.getTeamByName(this.site.getId(), parentName);
                if (parent != null)
                {
                    this.report.info("Adding team " + child.getName() + " to team " + parent.getName());
                    db.addTeamChild(parent, child);
                }
            }
        }
    }

    private void loadContacts(BergamotDB db)
    {
        for (ContactCfg configuration : this.config.getContacts())
        {
            if (! configuration.getTemplateBooleanValue())
            {
                if (ObjectState.isRemove(configuration.getObjectState()))
                {
                    this.removeContact(configuration, db);
                }
                else
                {
                    this.loadContact(configuration, db);
                }
            }
        }
        // load any contact where a template change cascades
        for (CascadedChange change : this.cascadedChanges.values())
        {
            if (change.dependent.getConfiguration() instanceof ContactCfg)
            {
                ContactCfg configuration = (ContactCfg) change.dependent.getConfiguration();
                if (! (configuration.getTemplateBooleanValue() || this.loadedObjects.contains("team:" + configuration.getName())))
                {
                    this.report.info("Reconfiguring contact " + configuration.getName() + " due to a change to the " + change.template.getName() + " inherited template.");
                    // first we need to resolve the inheritance for the cascaded object
                    db.getConfigResolver(this.site.getId()).computeInheritenance(configuration);
                    // load
                    this.loadContact(configuration, db);
                }
            }
        }
    }
    
    private void removeContact(ContactCfg configuration, BergamotDB db)
    {
        this.report.info("Removing contact: " + configuration.resolve().getName());
        Contact contact = db.getContactByName(this.site.getId(), configuration.resolve().getName());
        if (contact != null)
        {
            db.removeContact(contact.getId());
            db.removeConfig(contact.getId());
            db.removeSecurityDomainMembershipForCheck(contact.getId());
        }
    }
    
    private void loadContact(ContactCfg configuration, BergamotDB db)
    {
        ContactCfg resolvedConfiguration = configuration.resolve();
        // load
        Contact contact = db.getContactByName(this.site.getId(), resolvedConfiguration.getName());
        if (contact == null)
        {
            configuration.setId(this.site.randomObjectId());
            contact = new Contact();
            // handle the password
            if (this.online)
            {
                /*
                 * We are in online mode, ie: running from within the UI, 
                 * as such send a registration notification email.
                 */
                contact.resetPassword();
                this.registerContact(contact);
                this.report.info("Sending registration notification to " + resolvedConfiguration.getName() + " (" + resolvedConfiguration.getEmail() + ")");
            }
            else
            {
                /*
                 * We are in offline mode, ie: running an import from the CLI / first install, 
                 * so simply set a default password of 'bergamot' for any users,
                 * created.
                 */
                contact.hashPassword(this.defaultPassword);
                contact.setForcePasswordChange(this.requirePasswordChange);
                this.report.info("Setting default password for user " + resolvedConfiguration.getName() + " (" + resolvedConfiguration.getEmail() + ") to 'bergamot', please login and change it!");
            }
            this.report.info("Configuring new contact: " + resolvedConfiguration.getName() + " (" + resolvedConfiguration.getEmail() + ")");
        }
        else
        {
            configuration.setId(contact.getId());
            this.report.info("Reconfiguring existing contact: " + configuration.resolve().getName() + " (" + configuration.getId() + ")");
        }
        contact.configure(configuration);
        // notifications
        this.loadNotifications(contact.getId(), resolvedConfiguration.getNotifications(), db);
        // access controls
        this.loadAccessControls(contact.getId(), resolvedConfiguration.getAccessControls(), db);
        // store
        db.setContact(contact);
        this.loadedObjects.add("contact:" + configuration.getName());
        // teams
        for (String teamName : resolvedConfiguration.getTeams())
        {
            Team team = db.getTeamByName(this.site.getId(), teamName);
            if (team != null)
            {
                this.report.info("Adding contact " + contact.getName() + " to team " + team.getName());
                team.addContact(contact);
            }
        }
        // we need to rebuild the permissions
        this.rebuildPermissions = true;
        // security domains
        this.linkSecurityDomains(resolvedConfiguration, contact, db);
    }
    
    private void loadAccessControls(UUID roleId, List<AccessControlCfg> acl, BergamotDB db)
    {
        for (AccessControlCfg cfg : acl)
        {
            SecurityDomain domain = db.getSecurityDomainByName(this.site.getId(), cfg.getSecurityDomain());
            if (domain != null)
            {
                db.setAccessControl(new AccessControl(domain.getId(), roleId, new LinkedList<String>(cfg.getGrantedPermissions()), new LinkedList<String>(cfg.getRevokedPermissions())));
            }
        }
    }
    
    private void loadNotifications(UUID owner, NotificationsCfg configuration, BergamotDB db)
    {
        Notifications notifications = new Notifications();
        notifications.setId(owner);
        notifications.setEnabled(configuration.getEnabledBooleanValue());
        notifications.setAlertsEnabled(configuration.getAlertsBooleanValue());
        notifications.setRecoveryEnabled(configuration.getRecoveryBooleanValue());
        notifications.setAcknowledgeEnabled(configuration.getAcknowledgeBooleanValue());
        notifications.setIgnore(configuration.getIgnore().stream().map((e) -> {return Status.valueOf(e.toUpperCase());}).collect(Collectors.toList()));
        notifications.setAllEnginesEnabled(configuration.getAllEnginesEnabledBooleanValue());
        // load the time period
        if (! Util.isEmpty(configuration.getNotificationPeriod()))
        {
            TimePeriod timePeriod = db.getTimePeriodByName(this.site.getId(), configuration.getNotificationPeriod());
            if (timePeriod != null)
            {
                notifications.setTimePeriodId(timePeriod.getId());
            }
        }
        // updates
        UpdateNotificationsCfg updates = configuration.getUpdates();
        if (updates != null)
        {
            notifications.setUpdatesEnabled(updates.getEnabledBooleanValue());
            notifications.setUpdatesIgnore(updates.getIgnore().stream().map((e) -> {return Status.valueOf(e.toUpperCase());}).collect(Collectors.toList()));
            if (! Util.isEmpty(updates.getUpdatesPeriod()))
            {
                TimePeriod timePeriod = db.getTimePeriodByName(this.site.getId(), updates.getUpdatesPeriod());
                if (timePeriod != null)
                {
                    notifications.setUpdatesTimePeriodId(timePeriod.getId());
                }
            }
        }
        else
        {
            notifications.setUpdatesEnabled(false);
            notifications.setUpdatesTimePeriodId(null);
            notifications.setUpdatesIgnore(new LinkedList<Status>());
        }
        // store the notifications configuration
        db.setNotifications(notifications);
        // engines
        for (NotificationEngineCfg econfiguration : configuration.getNotificationEngines())
        {
            NotificationEngine notificationEngine = new NotificationEngine();
            notificationEngine.setNotificationsId(notifications.getId());
            notificationEngine.setEngine(econfiguration.getEngine());
            notificationEngine.setEnabled(econfiguration.getEnabledBooleanValue());
            notificationEngine.setAlertsEnabled(econfiguration.getAlertsBooleanValue());
            notificationEngine.setRecoveryEnabled(econfiguration.getRecoveryBooleanValue());
            notificationEngine.setUpdatesEnabled(econfiguration.getUpdatesBooleanValue());
            notificationEngine.setIgnore(econfiguration.getIgnore().stream().map(Status::parse).collect(Collectors.toList()));
            if (! Util.isEmpty(econfiguration.getNotificationPeriod()))
            {
                TimePeriod timePeriod = db.getTimePeriodByName(this.site.getId(), econfiguration.getNotificationPeriod());
                if (timePeriod != null)
                {
                    notificationEngine.setTimePeriodId(timePeriod.getId());
                }
            }
            //
            db.setNotificationEngine(notificationEngine);
        }
        // escalations
        db.removeEscalations(owner);
        for (EscalateCfg ecfg : configuration.getEscalations())
        {
            Escalation esc = new Escalation();
            esc.setNotificationsId(owner);
            esc.setAfter(ecfg.getAfterTimeInterval().toMillis());
            esc.setRenotify(ecfg.getRenotifyBooleanValue());
            esc.setIgnore(ecfg.getIgnore().stream().map(Status::parse).collect(Collectors.toList()));
            // load the time period
            if (! Util.isEmpty(ecfg.getEscalationPeriod()))
            {
                TimePeriod timePeriod = db.getTimePeriodByName(this.site.getId(), ecfg.getEscalationPeriod());
                if (timePeriod != null)
                {
                    notifications.setTimePeriodId(timePeriod.getId());
                }
            }
            // notify
            esc.getTeamIds().clear();
            esc.getContactIds().clear();
            if (ecfg.getNotify() != null)
            {
                for (String teamName : ecfg.getNotify().getTeams())
                {
                    Team team = db.getTeamByName(this.site.getId(), teamName);
                    if (team != null)
                    {
                        esc.getTeamIds().add(team.getId());
                    }
                }
                for (String contactName : ecfg.getNotify().getContacts())
                {
                    Contact contact = db.getContactByName(this.site.getId(), contactName);
                    if (contact != null)
                    {
                        esc.getContactIds().add(contact.getId());
                    }
                }
            }
            // store it
            db.setEscalation(esc);
        }
    }

    private void loadHosts(BergamotDB db)
    {
        // load all directly modified hosts
        for (HostCfg configuration : this.config.getHosts())
        {
            if (! configuration.getTemplateBooleanValue())
            {
                if (ObjectState.isRemove(configuration.getObjectState()))
                {
                    this.removeHost(configuration, db);
                }
                else
                {
                    this.loadHost(configuration, db);
                }
            }
        }
        // load any hosts where a template change cascades
        // Note: we don't need to separately handle service and trap 
        //       template changes as these are caught by reconfiguring 
        //       the host they exist on
        for (CascadedChange change : this.cascadedChanges.values())
        {
            if (change.dependent.getConfiguration() instanceof HostCfg)
            {
                HostCfg configuration = (HostCfg) change.dependent.getConfiguration();
                if (! (configuration.getTemplateBooleanValue() || this.loadedObjects.contains("host:" + configuration.getName())))
                {
                    this.report.info("Reconfiguring host " + configuration.getName() + " due to a change to the " + change.template.getName() + " inherited template.");
                    // first we need to resolve the inheritance for the cascaded object
                    db.getConfigResolver(this.site.getId()).computeInheritenance(configuration);
                    // load
                    this.loadHost(configuration, db);
                }
            }
        }
    }
    
    private void removeHost(HostCfg configuration, BergamotDB db)
    {
        // remove this host and all its services
        this.report.info("Removing host: " + configuration.resolve().getName());
        Host host = db.getHostByName(this.site.getId(), configuration.resolve().getName());
        if (host != null)
        {
            // remove all services
            for (Service service : host.getServices())
            {
                // remove
                this.unscheduleCheck(service);
                db.removeService(service.getId());
                db.removeConfig(service.getId());
                db.removeSecurityDomainMembershipForCheck(service.getId());
            }
            // remove the host
            this.unscheduleCheck(host);
            db.removeHost(host.getId());
            db.removeConfig(host.getId());
            db.removeSecurityDomainMembershipForCheck(host.getId());
        }
    }
    
    private void loadHost(HostCfg configuration, BergamotDB db)
    {
        HostCfg resolvedConfiguration = configuration.resolve();
        if (this.loadedObjects.contains("host:" + configuration.getName()))
        {
            this.report.info("Skipping reconfiguring host " + configuration.getName());
            return;
        }
        this.report.info("Loading host:\r\n" + resolvedConfiguration.toString());
        // load
        boolean newHost = false;
        Host host = db.getHostByName(this.site.getId(), resolvedConfiguration.getName());
        if (host == null)
        {
            configuration.setId(this.site.randomObjectId());
            host = new Host();
            newHost = true;
            this.report.info("Configuring new host: " + resolvedConfiguration.getName());
        }
        else
        {
            configuration.setId(host.getId());
            this.report.info("Reconfiguring existing host: " + resolvedConfiguration.getName() + " (" + configuration.getId() + ")");
        }
        host.configure(configuration);
        // load the check details
        this.loadActiveCheck(host, resolvedConfiguration, db);
        // add locations
        if (host.getLocationId() != null)
            db.invalidateHostsInLocation(host.getLocationId());
        host.setLocationId(null);
        String locationName = resolvedConfiguration.getLocation();
        if (!Util.isEmpty(locationName))
        {
            Location location = db.getLocationByName(this.site.getId(), locationName);
            if (location != null)
            {
                host.setLocationId(location.getId());
                db.invalidateHostsInLocation(location.getId());
            }
        }
        // add the host
        db.setHost(host);
        this.loadedObjects.add("host:" + configuration.getName());
        // schedule
        if (newHost)
        {
            this.scheduleCheck(host);
        }
        else
        {
            this.rescheduleCheck(host);
        }
        // remove any services and traps which are not
        if (! newHost)
        {
            // index the services we want
            Set<String> wantedServices = resolvedConfiguration.getServices().stream().map((s) -> s.resolve().getName()).collect(Collectors.toSet());
            // remove any unwanted services
            for (Service service : host.getServices())
            {
                if (! wantedServices.contains(service.getName()))
                {
                    report.info("Removing service " + service.getName() + " (" + service.getId() + ") as it is no longer given for this host");
                    // remove service
                    this.unscheduleCheck(service);
                    db.removeService(service.getId());
                    db.removeConfig(service.getId());
                }
            }
            // index the traps we want
            Set<String> wantedTraps = resolvedConfiguration.getTraps().stream().map((t) -> t.resolve().getName()).collect(Collectors.toSet());
            // remove any unwanted services
            for (Trap trap : host.getTraps())
            {
                if (! wantedTraps.contains(trap.getName()))
                {
                    report.info("Removing trap " + trap.getName() + " (" + trap.getId() + ") as it is no longer given for this host");
                    // remove trap
                    db.removeTrap(trap.getId());
                    db.removeConfig(trap.getId());
                }
            }
        }
        // add services
        for (ServiceCfg serviceConfiguration : resolvedConfiguration.getServices())
        {
            if (ObjectState.isRemove(serviceConfiguration.getObjectState()))
            {
                this.removeService(host, serviceConfiguration, db);
            }
            else
            {
                this.loadService(host, serviceConfiguration, db);
            }
        }
        // add traps
        for (TrapCfg trapConfiguration : resolvedConfiguration.getTraps())
        {
            if (ObjectState.isRemove(trapConfiguration.getObjectState()))
            {
                this.removeTrap(host, trapConfiguration, db);
            }
            else
            {
                this.loadTrap(host, trapConfiguration, db);
            }
        }
        // cache invalidation
        db.invalidateServicesOnHost(host.getId());
        db.invalidateTrapsOnHost(host.getId());
    }
    
    private void loadActiveCheck(ActiveCheck<?,?> check, ActiveCheckCfg<?> resolvedConfiguration, BergamotDB db)
    {
        this.loadRealCheck(check, resolvedConfiguration, db);
        // the check period
        if (! Util.isEmpty(resolvedConfiguration.getSchedule().getTimePeriod()))
        {
            TimePeriod timePeriod = db.getTimePeriodByName(this.site.getId(), resolvedConfiguration.getSchedule().getTimePeriod());
            if (timePeriod != null)
            {
                check.setTimePeriodId(timePeriod.getId());
            }
        }        
    }
    
    private void loadCheckState(Check<?,?> check, CheckCfg<?> configuration, BergamotDB db)
    {
        CheckState state = db.getCheckState(check.getId());
        if (state == null || this.resetState)
        {
            state = new CheckState();
            state.setCheckId(check.getId());
            state.configure(configuration);
            db.setCheckState(state);
        }
    }
    
    private void loadCheckStats(Check<?,?> check, CheckCfg<?> configuration, BergamotDB db)
    {
        CheckStats stats = db.getCheckStats(check.getId());
        if (stats == null || this.resetState)
        {
            stats = new CheckStats();
            stats.setCheckId(check.getId());
            db.setCheckStats(stats);
        }
    }
    
    private void loadRealCheck(RealCheck<?,?> check, RealCheckCfg<?> resolvedConfiguration, BergamotDB db)
    {
        this.loadCheck(check, resolvedConfiguration, db);
        // the check command
        if (resolvedConfiguration.getCheckCommand() != null)
        {
            // lookup the command
            Command command = db.getCommandByName(this.site.getId(), resolvedConfiguration.getCheckCommand().getCommand());
            if (command != null)
            {
                CheckCommand checkCommand = new CheckCommand();
                checkCommand.setCheckId(check.getId());
                checkCommand.configure(resolvedConfiguration.getCheckCommand());
                checkCommand.setCommandId(command.getId());
                db.setCheckCommand(checkCommand);
                this.report.info("Added command " + command.getName() + " to check " + check.getName());
            }
            else
            {
                throw new DataException("The command " + resolvedConfiguration.getCheckCommand().getCommand() + " could not be found, needed by " + check.getName());
            }
        }
        // track any resource pools we need to update
        if (! Util.isEmpty(resolvedConfiguration.getResourcePool()))
        {
            this.updatedResourcePools.add(resolvedConfiguration.getResourcePool());
        }
    }
    
    private void loadSLAs(Check<?,?> check, CheckCfg<?> resolvedConfiguration, BergamotDB db)
    {
        for (SLACfg slaCfg : resolvedConfiguration.getSlas())
        {
            SLA sla = db.getSLAByName(check.getId(), slaCfg.getName());
            if (sla == null) sla = new SLA().forCheck(check);
            // configure the SLA
            sla.configure(slaCfg);
            // upsert the SLA
            db.setSLA(sla);
            // configure the periods
            for (SLAPeriodCfg spCfg : slaCfg.getPeriods())
            {
                if (spCfg instanceof SLARollingPeriodCfg)
                {
                    SLARollingPeriod srp = db.getSLARollingPeriodByName(sla.getId(), spCfg.getName());
                    if (srp == null) srp = new SLARollingPeriod().forSLA(sla);
                    srp.configure((SLARollingPeriodCfg) spCfg);
                    db.setSLARollingPeriod(srp);
                }
                else if (spCfg instanceof SLAFixedPeriodCfg)
                {
                    SLAFixedPeriod srp = db.getSLAFixedPeriodByName(sla.getId(), spCfg.getName());
                    if (srp == null) srp = new SLAFixedPeriod().forSLA(sla);
                    srp.configure((SLAFixedPeriodCfg) spCfg);
                    db.setSLAFixedPeriod(srp);
                }
            }
        }
    }

    private void loadCheck(Check<?,?> check, CheckCfg<?> resolvedConfiguration, BergamotDB db)
    {
        // set the processing pool
        check.setPool(this.site.computeProcessingPool(check.getId()));
        // the state
        this.loadCheckState(check, resolvedConfiguration, db);
        // the stats
        this.loadCheckStats(check, resolvedConfiguration, db);
        // the SLAs
        this.loadSLAs(check, resolvedConfiguration, db);
        // notifications
        this.loadNotifications(check.getId(), resolvedConfiguration.getNotifications(), db);
        // notify
        check.getTeamIds().clear();
        check.getContactIds().clear();
        for (String teamName : resolvedConfiguration.getNotify().getTeams())
        {
            Team team = db.getTeamByName(this.site.getId(), teamName);
            if (team != null)
            {
                check.getTeamIds().add(team.getId());
            }
        }
        for (String contactName : resolvedConfiguration.getNotify().getContacts())
        {
            Contact contact = db.getContactByName(this.site.getId(), contactName);
            if (contact != null)
            {
                check.getContactIds().add(contact.getId());
            }
        }
        // the groups
        for (UUID oldGroupId : check.getGroupIds())
        {
            db.invalidateChecksInGroup(oldGroupId);
        }
        check.getGroupIds().clear();
        for (String groupName : resolvedConfiguration.getGroups())
        {
            Group group = db.getGroupByName(this.site.getId(), groupName);
            if (group != null)
            {
                this.report.info("Adding check " + check.getName() + " to group " + group.getName());
                check.getGroupIds().add(group.getId());
                db.invalidateChecksInGroup(group.getId());
            }
        }
        // security domains
        this.linkSecurityDomains(resolvedConfiguration, check, db);
    }
    
    private void removeService(Host host, ServiceCfg configuration, BergamotDB db)
    {
        this.report.info("Removing service: " + configuration.resolve().getName() + " on host " + host.getName());
        Service service = db.getServiceOnHost(host.getId(), configuration.resolve().getName());
        if (service != null)
        {
            // remove service
            this.unscheduleCheck(service);
            db.removeService(service.getId());
            db.removeConfig(service.getId());
            db.removeSecurityDomainMembershipForCheck(service.getId());
        }
    }

    private void loadService(Host host, ServiceCfg configuration, BergamotDB db)
    {
        // resolve
        ServiceCfg resolvedConfiguration = configuration.resolve();
        // create the service
        boolean newService = false;
        Service service = db.getServiceOnHost(host.getId(), resolvedConfiguration.getName());
        if (service == null)
        {
            configuration.setId(this.site.randomObjectId());
            service = new Service();
            service.setHostId(host.getId());
            newService = true;
            this.report.info("Configuring new service: " + configuration.resolve().getName() + " on host " + host.getName());
        }
        else
        {
            configuration.setId(service.getId());
            this.report.info("Reconfiguring existing service: " + configuration.resolve().getName() + " on host " + host.getName() + " (" + configuration.getId() + ")");
        }
        service.configure(configuration);
        // load the check details
        this.loadActiveCheck(service, resolvedConfiguration, db);
        // add
        db.setService(service);
        // schedule
        if (newService)
        {
            // schedule this new check
            this.scheduleCheck(service);
        }
        else
        {
            // reschedule this reconfigured check
            this.rescheduleCheck(service);
        }
    }
    
    private void removeTrap(Host host, TrapCfg configuration, BergamotDB db)
    {
        this.report.info("Removing trap: " + configuration.resolve().getName() + " on host " + host.getName());
        Trap trap = db.getTrapOnHost(host.getId(), configuration.getName());
        if (trap != null)
        {
            db.removeTrap(trap.getId());
            db.removeConfig(trap.getId());
            db.removeSecurityDomainMembershipForCheck(trap.getId());
        }
    }
    
    private void loadTrap(Host host, TrapCfg configuration, BergamotDB db)
    {
        // resolve
        TrapCfg resolvedConfiguration = configuration.resolve();
        // create the service
        Trap trap = db.getTrapOnHost(host.getId(), configuration.getName());
        if (trap == null)
        {
            configuration.setId(this.site.randomObjectId());
            trap = new Trap();
            trap.setHostId(host.getId());
            this.report.info("Configuring new trap: " + configuration.resolve().getName() + " on host " + host.getName());
        }
        else
        {
            configuration.setId(trap.getId());
            this.report.info("Reconfiguring existing trap: " + configuration.resolve().getName() + " on host " + host.getName() + " (" + configuration.getId() + ")");
        }
        trap.configure(configuration);
        // load the check details
        this.loadPasiveCheck(trap, resolvedConfiguration, db);
        // add
        db.setTrap(trap);
    }
    
    private void loadPasiveCheck(PassiveCheck<?,?> check, PassiveCheckCfg<?> resolvedConfiguration, BergamotDB db)
    {
        this.loadRealCheck(check, resolvedConfiguration, db);
    }
    
    private void loadVirtualCheck(VirtualCheck<?,?> check, VirtualCheckCfg<?> resolvedConfiguration, BergamotDB db)
    {
        this.loadCheck(check, resolvedConfiguration, db);
        // parse the condition
        if (! Util.isEmpty(resolvedConfiguration.getCondition()))
        {
            VirtualCheckOperator cond = VirtualCheckExpressionParser.parseVirtualCheckExpression(resolvedConfiguration.getCondition());
            if (cond != null)
            {
                // context to use
                VirtualCheckExpressionContext vcec = db.createVirtualCheckContext(this.site.getId(), null);
                // validate the condition
                for (CheckReference<?> chkRef : cond.computeDependencies(vcec))
                {
                    if (chkRef.resolve(vcec) == null)
                    {
                        throw new RuntimeException("The virtual check " + check.getType() + " " + check.getName() + " is referencing checks which do not exist: " + chkRef.toString());
                    }
                }
                // set the condition
                check.setCondition(cond);
                this.report.info("Using virtual check condition " + cond.toString() + " for " + check);
                // extract and set the dependencies for this check
                this.setVirtualCheckDependencies(check, vcec);
            }
        }
    }
    
    private void setVirtualCheckDependencies(VirtualCheck<?,?> check, VirtualCheckExpressionContext vcec)
    {
        VirtualCheckOperator cond = check.getCondition();
        // cross reference the checks
        check.setReferenceIds(new LinkedList<UUID>(cond.computeDependencies(vcec).stream().map((c) -> c.resolve(vcec).getId()).collect(Collectors.toSet())));
        check.setReferenceResourcePools(new LinkedList<String>(cond.computePoolDependencies(vcec)));        
    }

    private void loadClusters(BergamotDB db)
    {
        for (ClusterCfg configuration : this.config.getClusters())
        {
            if (!configuration.getTemplateBooleanValue())
            {
                if (ObjectState.isRemove(configuration.getObjectState()))
                {
                    this.removeCluster(configuration, db);
                }
                else
                {
                    this.loadCluster(configuration, db);
                }
            }
        }
        // load any clusters where a template change cascades
        // Note: we don't need to separately handle resources 
        //       template changes as these are caught by reconfiguring 
        //       the cluster they exist on
        for (CascadedChange change : this.cascadedChanges.values())
        {
            if (change.dependent.getConfiguration() instanceof ClusterCfg)
            {
                ClusterCfg configuration = (ClusterCfg) change.dependent.getConfiguration();
                if (! (configuration.getTemplateBooleanValue() || this.loadedObjects.contains("cluster:" + configuration.getName())))
                {
                    this.report.info("Reconfiguring cluster " + configuration.getName() + " due to a change to the " + change.template.getName() + " inherited template.");
                    // first we need to resolve the inheritance for the cascaded object
                    db.getConfigResolver(this.site.getId()).computeInheritenance(configuration);
                    // load
                    this.loadCluster(configuration, db);
                }
            }
        }
    }
    
    private void removeCluster(ClusterCfg configuration, BergamotDB db)
    {
        this.report.info("Removing cluster: " + configuration.resolve().getName());
        Cluster cluster = db.getClusterByName(this.site.getId(), configuration.getName());
        if (cluster != null)
        {
            for (Resource resource : cluster.getResources())
            {
                db.removeResource(resource.getId());
                db.removeConfig(resource.getId());
                db.removeSecurityDomainMembershipForCheck(cluster.getId());
            }
            db.removeCluster(cluster.getId());
            db.removeConfig(cluster.getId());
            db.removeSecurityDomainMembershipForCheck(cluster.getId());
        }
    }
    
    private void loadCluster(ClusterCfg configuration, BergamotDB db)
    {
        if (this.loadedObjects.contains("cluster:" + configuration.getName()))
        {
            this.report.info("Skipping reconfiguring cluster " + configuration.getName());
            return;
        }
        // resolved config
        ClusterCfg resolvedConfiguration = configuration.resolve();
        // load
        boolean newCluster = false;
        Cluster cluster = db.getClusterByName(this.site.getId(), configuration.getName());
        if (cluster == null)
        {
            configuration.setId(this.site.randomObjectId());
            cluster = new Cluster();
            newCluster = true;
            this.report.info("Configuring new cluster: " + configuration.resolve().getName());
        }
        else
        {
            configuration.setId(cluster.getId());
        }
        cluster.configure(configuration);
        // load the check details
        this.loadVirtualCheck(cluster, resolvedConfiguration, db);
        // add the cluster
        db.setCluster(cluster);
        this.loadedObjects.add("cluster:" + configuration.getName());
        // remove any resources which are not wanted
        if (! newCluster)
        {
            // index the resource we want
            Set<String> wantedResources = resolvedConfiguration.getResources().stream().map((r) -> r.resolve().getName()).collect(Collectors.toSet());
            // remove any unwanted services
            for (Resource resource : cluster.getResources())
            {
                if (! wantedResources.contains(resource.getName()))
                {
                    report.info("Removing resource " + resource.getName() + " (" + resource.getId() + ") as it is no longer given for this cluster");
                    // remove resource
                    db.removeResource(resource.getId());
                    db.removeConfig(resource.getId());
                }
            }
        }
        // add resources
        for (ResourceCfg resourceConfiguration : resolvedConfiguration.getResources())
        {
            if (ObjectState.isRemove(resourceConfiguration.getObjectState()))
            {
                this.removeResource(cluster, resourceConfiguration, db);
            }
            else
            {
                this.loadResource(cluster, resourceConfiguration, db);
            }
        }
        // cache invalidation
        db.invalidateResourcesOnCluster(cluster.getId());
    }
    
    private void removeResource(Cluster cluster, ResourceCfg configuration, BergamotDB db)
    {
        this.report.info("Removing resource: " + configuration.resolve().getName() + " on cluster " + cluster.getName());
        Resource resource = db.getResourceOnCluster(cluster.getId(), configuration.resolve().getName());
        if (resource != null)
        {
            db.removeResource(resource.getId());
            db.removeConfig(resource.getId());
            db.removeSecurityDomainMembershipForCheck(resource.getId());
        }
    }
    
    private void loadResource(Cluster cluster, ResourceCfg configuration, BergamotDB db)
    {
        // resolve
        ResourceCfg resolvedConfiguration = configuration.resolve();
        // create the service
        Resource resource = db.getResourceOnCluster(cluster.getId(), resolvedConfiguration.getName());
        if (resource == null)
        {
            configuration.setId(this.site.randomObjectId());
            resource = new Resource();
            resource.setClusterId(cluster.getId());
            this.report.info("Configuring new resource: " + configuration.resolve().getName() + " on cluster " + cluster.getName());
        }
        else
        {
            configuration.setId(resource.getId());
            this.report.info("Reconfiguring existing group: " + configuration.resolve().getName() + " on cluster " + cluster.getName() + " (" + configuration.getId() + ")");
        }
        resource.configure(configuration);
        // load the check details
        this.loadVirtualCheck(resource, resolvedConfiguration, db);
        // add
        db.setResource(resource);
    }
    
    protected void linkSecurityDomains(SecuredObjectCfg<?> resolvedConfiguration, SecuredObject<?,?> object, BergamotDB db)
    {
        this.linkSecurityDomains(resolvedConfiguration, object.getId(), db);
    }
    
    protected void linkSecurityDomains(SecuredObjectCfg<?> resolvedConfiguration, UUID objectId, BergamotDB db)
    {
        // set security domain membership
        db.removeSecurityDomainMembershipForCheck(objectId);
        for (String securityDomainName : resolvedConfiguration.getSecurityDomains())
        {
            SecurityDomain domain = db.getSecurityDomainByName(this.site.getId(), securityDomainName);
            if (domain != null)
                db.addCheckToSecurityDomain(domain.getId(), objectId);
        }
        // ensure we flush cached permissions
        this.clearPermissionsCache = true;
    }
    
    protected void linkDependencies(BergamotDB db)
    {
        for (List<? extends TemplatedObjectCfg<?>> objects : this.config.getAllObjects())
        {
            for (TemplatedObjectCfg<?> object : objects)
            {
                if ((! object.getTemplateBooleanValue()) && object instanceof RealCheckCfg<?>)
                {
                    RealCheckCfg<?> checkCfg = (RealCheckCfg<?>) object;
                    if (! Util.isEmpty(checkCfg.getDepends()))
                    {
                        // we only need to link dependencies on host and cluster
                        // as services auto depend on host and resources auto depend on cluster
                        if (checkCfg instanceof HostCfg)
                        {
                            Host host = db.getHostByName(this.site.getId(), checkCfg.getName());
                            // did we find the check
                            if (host != null)
                            {
                                // parse the dependencies
                                List<CheckReference<?>> dependsOn = VirtualCheckExpressionParser.parseParentsExpression(checkCfg.getDepends());
                                // validate the references and link
                                VirtualCheckExpressionContext vcec = db.createVirtualCheckContext(this.site.getId(), host);
                                for (CheckReference<?> chkRef : dependsOn)
                                {
                                    Check<?,?> dependsOnCheck = chkRef.resolve(vcec);
                                    if (dependsOnCheck == null) throw new RuntimeException("The check " + checkCfg.getName() + " depends upon the check " + chkRef + " which does not exist");
                                    host.getDependsIds().add(dependsOnCheck.getId());
                                }
                                //
                                this.report.info("The check " + checkCfg.getName() + "(" + host.getId() + ") depends upon " + host.getDependsIds());
                                // update
                                db.setCheck(host);
                            }
                        }
                    }
                }
            }
        }
    }
    
    private void updateResourcePools(BergamotDB db)
    {
        for (String resourcePool : this.updatedResourcePools)
        {
            this.report.info("Updating virtual checks dependent on the resource pool: " + resourcePool);
            // virtual check context for resolving checks
            VirtualCheckExpressionContext vcec = db.createVirtualCheckContext(this.site.getId(), null);
            // update clusters
            for (Cluster cluster : db.getClusterReferencingResourcePool(this.site.getId(), resourcePool))
            {
                this.report.info("Updating cluster " + cluster.getName() + " (" + cluster.getId() + ")");
                this.setVirtualCheckDependencies(cluster, vcec);
                db.setCluster(cluster);
            }
            // update resources
            for (Resource resource : db.getResourcesReferencingResourcePool(this.site.getId(), resourcePool))
            {
                this.report.info("Updating resource " + resource.getName() + " (" + resource.getId() + ")");
                this.setVirtualCheckDependencies(resource, vcec);
                db.setResource(resource);
            }
        }
    }
    
    private void loadCredentials(BergamotDB db)
    {
        for (CredentialCfg configuration : this.config.getCredentials())
        {
            if (! configuration.getTemplateBooleanValue())
            {
                if (ObjectState.isRemove(configuration.getObjectState()))
                {
                    // remove the credential
                    this.removeCredential(configuration, db);
                }
                else
                {
                    // add or change the credential
                    this.loadCredential(configuration, db);
                }
            }
        }
        // load any credential where a template change cascades
        for (CascadedChange change : this.cascadedChanges.values())
        {
            if (change.dependent.getConfiguration() instanceof CredentialCfg)
            {
                CredentialCfg configuration = (CredentialCfg) change.dependent.getConfiguration();
                if (! (configuration.getTemplateBooleanValue() || this.loadedObjects.contains("credential:" + configuration.getName())))
                {
                    this.report.info("Reconfiguring credential " + configuration.getName() + " due to a change to the " + change.template.getName() + " inherited template.");
                    // first we need to resolve the inheritance for the cascaded object
                    db.getConfigResolver(this.site.getId()).computeInheritenance(configuration);
                    // load
                    this.loadCredential(configuration, db);
                }
            }
        }
    }
    
    private void removeCredential(CredentialCfg configuration, BergamotDB db)
    {
        this.report.info("Removing credential: " + configuration.resolve().getName());
        // remove the location
        Credential credential = db.getCredentialByName(this.site.getId(), configuration.getName());
        if (credential != null)
        {
            db.removeCredential(credential.getId());
            db.removeConfig(credential.getId());
            db.removeSecurityDomainMembershipForCheck(credential.getId());
        }
    }
    
    private void loadCredential(CredentialCfg configuration, BergamotDB db)
    {
        if (this.loadedObjects.contains("credential:" + configuration.getName()))
        {
            this.report.info("Skipping reconfiguring credential " + configuration.getName());
            return;
        }
        // load
        CredentialCfg resolvedConfiguration = configuration.resolve();
        Credential credential = db.getCredentialByName(this.site.getId(), configuration.getName());
        if (credential == null)
        {
            configuration.setId(this.site.randomObjectId());
            credential = new Credential();
            this.report.info("Configuring new credential: " + resolvedConfiguration.getName());
        }
        else
        {
            configuration.setId(credential.getId());
            this.report.info("Reconfiguring existing credential: " + resolvedConfiguration.getName() + " (" + configuration.getId() + ")");
        }
        credential.configure(configuration);
        db.setCredential(credential);
        this.loadedObjects.add("credential:" + configuration.getName());
        // security domains
        this.linkSecurityDomains(resolvedConfiguration, credential, db);
    }
    
    /**
     * Queue the given contact to have a registration email sent
     */
    private void registerContact(Contact contact)
    {
        this.delayedContactRegistrations.add(contact);
    }
    
    private void scheduleCheck(ActiveCheck<?,?> check)
    {
        this.report.info("Sscheduling " + check.getType() + " " + check.getName() + " (" + check.getId() + ")");
        this.delayedSchedulerActions.add(new DelayedSchedulerAction(new SchedulerKey(check.getSiteId(), check.getPool()), new ScheduleCheck(check.getId())));
        this.delayedSchedulerActions.add(new DelayedSchedulerAction(new SchedulerKey(check.getSiteId(), check.getPool()), new EnableCheck(check.getId())));
    }
    
    private void rescheduleCheck(ActiveCheck<?,?> check)
    {
        this.report.info("Rescheduling " + check.getType() + " " + check.getName() + " (" + check.getId() + ")");
        this.delayedSchedulerActions.add(new DelayedSchedulerAction(new SchedulerKey(check.getSiteId(), check.getPool()), new RescheduleCheck(check.getId())));
        this.delayedSchedulerActions.add(new DelayedSchedulerAction(new SchedulerKey(check.getSiteId(), check.getPool()), new EnableCheck(check.getId())));
    }
    
    private void unscheduleCheck(ActiveCheck<?,?> check)
    {
        this.report.info("Unscheduling " + check.getType() + " " + check.getName() + " (" + check.getId() + ")");
        this.delayedSchedulerActions.add(new DelayedSchedulerAction(new SchedulerKey(check.getSiteId(), check.getPool()), new UnscheduleCheck(check.getId())));
    }
    
    private static class CascadedChange
    {
        public final Config dependent;
        
        public final Config template;
        
        @SuppressWarnings("unused")
        public final TemplatedObjectCfg<?> change;
       
        public CascadedChange(TemplatedObjectCfg<?> change, Config template, Config dependent)
        {
            this.change = change;
            this.template = template;
            this.dependent = dependent;
        }
    }
    
    public static class DelayedSchedulerAction
    {
        public SchedulerKey key;
        
        public SchedulerAction action;
        
        public DelayedSchedulerAction(SchedulerKey key, SchedulerAction action)
        {
            this.key = key;
            this.action = action;
        }
    }
}
