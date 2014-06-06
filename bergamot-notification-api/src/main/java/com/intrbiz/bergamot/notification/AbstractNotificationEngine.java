package com.intrbiz.bergamot.notification;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.intrbiz.bergamot.config.NotificationEngineCfg;
import com.intrbiz.bergamot.model.message.ClusterMO;
import com.intrbiz.bergamot.model.message.HostMO;
import com.intrbiz.bergamot.model.message.ResourceMO;
import com.intrbiz.bergamot.model.message.ServiceMO;
import com.intrbiz.bergamot.model.message.TrapMO;
import com.intrbiz.bergamot.model.message.notification.Notification;
import com.intrbiz.express.DefaultContext;
import com.intrbiz.express.ExpressContext;
import com.intrbiz.express.ExpressEntityResolver;
import com.intrbiz.express.ExpressExtensionRegistry;
import com.intrbiz.express.action.ActionHandler;
import com.intrbiz.express.value.ValueExpression;

public abstract class AbstractNotificationEngine implements NotificationEngine
{
    private Logger logger = Logger.getLogger(AbstractNotificationEngine.class);
    
    private final String name;

    protected NotificationEngineCfg config;

    protected Notifier notifier;

    protected final ExpressExtensionRegistry expressExtensions = new ExpressExtensionRegistry("bergamot").addSubRegistry(ExpressExtensionRegistry.getDefaultRegistry());

    protected final Map<String, ValueExpression> templates = new HashMap<String, ValueExpression>();

    public AbstractNotificationEngine(String name)
    {
        super();
        this.name = name;
    }

    @Override
    public void configure(NotificationEngineCfg cfg) throws Exception
    {
        this.config = cfg;
        // load templates
        for (Entry<String, String> template : this.getDefaultTemplates().entrySet())
        {
            String name = template.getKey();
            String theTemplate = cfg.getStringParameterValue(name, template.getValue());
            this.addTemplate(name, theTemplate);
        }
        // configure
        this.configure();
        // debug templates
        for (Entry<String, ValueExpression> template : this.templates.entrySet())
        {
            logger.debug("Using template: " + template.getKey() + " => " + template.getValue().toString());
        }
    }

    protected void configure() throws Exception
    {
    }
    
    protected Map<String, String> getDefaultTemplates()
    {
        return new HashMap<String, String>();
    }

    @Override
    public NotificationEngineCfg getConfiguration()
    {
        return this.config;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public Notifier getNotifier()
    {
        return this.notifier;
    }

    @Override
    public void setNotifier(Notifier notifier)
    {
        this.notifier = notifier;
    }

    public Map<String, ValueExpression> getTemplates()
    {
        return templates;
    }

    public void setTemplates(Map<String, ValueExpression> templates)
    {
        this.templates.clear();
        if (templates != null)
            this.templates.putAll(templates);
    }

    public void addTemplate(String name, String template)
    {
        this.addTemplate(name, new ValueExpression(this.createContext(null), template));
    }

    public void addTemplate(String name, ValueExpression template)
    {
        this.templates.put(name, template);
    }

    public ValueExpression getTemplate(String name)
    {
        return this.templates.get(name);
    }

    public String applyTemplate(String name, Notification notification)
    {
        ValueExpression vexp = this.getTemplate(name);
        if (vexp != null)
        {
            return String.valueOf(vexp.get(this.createContext(notification), notification));
        }
        throw new NotificationException("Failed to find template: " + name);
    }

    protected ExpressContext createContext(Notification notification)
    {
        ExpressContext ctx = new DefaultContext(this.expressExtensions, new ExpressEntityResolver()
        {
            @Override
            public Object getEntity(String name, Object source)
            {
                if (notification != null)
                {
                    if ("this".equals(name))
                    {
                        return notification;
                    }
                    else if ("check".equals(name))
                    {
                        return notification.getCheck();
                    }
                    else if ("service".equals(name) && (notification.getCheck() instanceof ServiceMO))
                    {
                        return notification.getCheck();
                    }
                    else if ("trap".equals(name) && (notification.getCheck() instanceof TrapMO))
                    {
                        return notification.getCheck();
                    }
                    else if ("resource".equals(name) && (notification.getCheck() instanceof ResourceMO))
                    {
                        return notification.getCheck();
                    }
                    else if ("host".equals(name) && (notification.getCheck() instanceof HostMO))
                    {
                        return notification.getCheck();
                    }
                    else if ("host".equals(name) && (notification.getCheck() instanceof ServiceMO))
                    {
                        return ((ServiceMO) notification.getCheck()).getHost();
                    }
                    else if ("host".equals(name) && (notification.getCheck() instanceof TrapMO))
                    {
                        return ((TrapMO) notification.getCheck()).getHost();
                    }
                    else if ("cluster".equals(name) && (notification.getCheck() instanceof ClusterMO))
                    {
                        return notification.getCheck();
                    }
                    else if ("cluster".equals(name) && (notification.getCheck() instanceof ResourceMO)) { return ((ResourceMO) notification.getCheck()).getCluster(); }
                }
                return null;
            }

            @Override
            public ActionHandler getAction(String name, Object source)
            {
                return null;
            }
        });
        return ctx;
    }
}