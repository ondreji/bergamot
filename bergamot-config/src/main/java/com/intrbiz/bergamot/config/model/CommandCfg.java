package com.intrbiz.bergamot.config.model;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.intrbiz.bergamot.config.resolver.ResolveWith;
import com.intrbiz.bergamot.config.resolver.stratergy.CoalesceEmptyString;

@XmlType(name = "command")
@XmlRootElement(name = "command")
public class CommandCfg extends NamedObjectCfg<CommandCfg>
{
    private static final long serialVersionUID = 1L;
    
    private String engine;

    private String executor;
    
    private String category;
    
    private String application;

    public CommandCfg()
    {
        super();
    }

    @XmlAttribute(name = "engine")
    @ResolveWith(CoalesceEmptyString.class)
    public String getEngine()
    {
        return engine;
    }

    public void setEngine(String engine)
    {
        this.engine = engine;
    }

    @XmlAttribute(name = "executor")
    @ResolveWith(CoalesceEmptyString.class)
    public String getExecutor()
    {
        return executor;
    }

    public void setExecutor(String executor)
    {
        this.executor = executor;
    }

    @XmlAttribute(name = "category")
    @ResolveWith(CoalesceEmptyString.class)
    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    @XmlAttribute(name = "application")
    @ResolveWith(CoalesceEmptyString.class)
    public String getApplication()
    {
        return application;
    }

    public void setApplication(String application)
    {
        this.application = application;
    }

    public List<TemplatedObjectCfg<?>> getTemplatedChildObjects()
    {
        List<TemplatedObjectCfg<?>> r = new LinkedList<TemplatedObjectCfg<?>>();
        return r;
    }
}
