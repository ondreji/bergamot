package com.intrbiz.bergamot.config;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "database")
@XmlRootElement(name = "database")
public class DatabaseCfg
{
    private String url;
    
    private String username;
    
    private String password;
    
    public DatabaseCfg()
    {
        super();
    }
    
    public DatabaseCfg(String url)
    {
        super();
        this.url = url;
    }
    
    public DatabaseCfg(String url, String username, String password)
    {
        super();
        this.url = url;
        this.username = username;
        this.password = password;
    }

    @XmlAttribute(name = "url")
    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }
    
    @XmlAttribute(name = "username")
    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    @XmlAttribute(name = "password")
    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }
}
