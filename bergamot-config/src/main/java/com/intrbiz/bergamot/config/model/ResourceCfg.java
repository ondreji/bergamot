package com.intrbiz.bergamot.config.model;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "resource")
@XmlRootElement(name = "resource")
public class ResourceCfg extends VirtualCheckCfg<ResourceCfg>
{
    public ResourceCfg()
    {
        super();
    }
}