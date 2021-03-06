package com.intrbiz.bergamot.config.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.intrbiz.bergamot.config.resolver.ResolveWith;
import com.intrbiz.bergamot.config.resolver.stratergy.Coalesce;

@XmlType(name = "rolling-period")
@XmlRootElement(name = "rolling-period")
public class SLARollingPeriodCfg extends SLAPeriodCfg
{
    private static final long serialVersionUID = 1L;
    
    public enum RollingPeriodGranularityCfg {
        daily,
        weekly,
        monthly,
        yearly
    }
    
    private RollingPeriodGranularityCfg granularity;

    public SLARollingPeriodCfg()
    {
        super();
    }

    @XmlAttribute(name = "granularity")
    @ResolveWith(Coalesce.class)
    public RollingPeriodGranularityCfg getGranularity()
    {
        return granularity;
    }

    public void setGranularity(RollingPeriodGranularityCfg granularity)
    {
        this.granularity = granularity;
    }
}
