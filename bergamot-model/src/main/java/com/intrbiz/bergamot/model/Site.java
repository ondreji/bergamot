package com.intrbiz.bergamot.model;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.intrbiz.bergamot.data.BergamotDB;
import com.intrbiz.bergamot.model.message.SiteMO;
import com.intrbiz.data.db.compiler.meta.SQLColumn;
import com.intrbiz.data.db.compiler.meta.SQLPrimaryKey;
import com.intrbiz.data.db.compiler.meta.SQLTable;
import com.intrbiz.data.db.compiler.meta.SQLUnique;
import com.intrbiz.data.db.compiler.meta.SQLVersion;

/**
 * A monitoring 'site'
 */
@SQLTable(schema = BergamotDB.class, name = "site", since = @SQLVersion({ 1, 0, 0 }))
public final class Site extends BergamotObject<SiteMO>
{
    @SQLColumn(index = 1, name = "id", since = @SQLVersion({ 1, 0, 0 }))
    @SQLPrimaryKey()
    protected UUID id;

    @SQLColumn(index = 2, name = "name", notNull = true, since = @SQLVersion({ 1, 0, 0 }))
    @SQLUnique()
    protected String name;

    @SQLColumn(index = 3, name = "summary", notNull = true, since = @SQLVersion({ 1, 0, 0 }))
    protected String summary;

    @SQLColumn(index = 4, name = "description", since = @SQLVersion({ 1, 0, 0 }))
    protected String description;

    @SQLColumn(index = 5, name = "aliases", type = "TEXT[]", since = @SQLVersion({ 1, 0, 0 }))
    protected List<String> aliases = new LinkedList<String>();

    public Site()
    {
        super();
    }

    public UUID getId()
    {
        return id;
    }

    public void setId(UUID id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getSummary()
    {
        return summary;
    }

    public void setSummary(String summary)
    {
        this.summary = summary;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public List<String> getAliases()
    {
        return aliases;
    }

    public void setAliases(List<String> aliases)
    {
        this.aliases = aliases;
    }

    public SiteMO toMO(boolean stub)
    {
        SiteMO mo = new SiteMO();
        mo.setId(this.getId());
        mo.setName(this.getName());
        mo.setSummary(this.getSummary());
        mo.setDescription(this.getDescription());
        mo.setAliases(this.getAliases());
        return mo;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Site other = (Site) obj;
        if (id == null)
        {
            if (other.id != null) return false;
        }
        else if (!id.equals(other.id)) return false;
        return true;
    }
    
    public UUID randomObjectId()
    {
        return randomId(this.getId());
    }

    /**
     * Generate a new random site id, site ids only use the upper 48 bits.
     * 
     * @return the site id
     */
    public static UUID randomSiteId()
    {
        return getSiteId(UUID.randomUUID());
    }

    /**
     * Generate a random object id within the given site
     * 
     * @param siteId
     * @return
     */
    public static UUID randomId(UUID siteId)
    {
        return setSiteId(siteId, UUID.randomUUID());
    }

    /**
     * Get the site id for the given object id
     * 
     * @param objectId
     * @return
     */
    public static UUID getSiteId(UUID objectId)
    {
        return new UUID((objectId.getMostSignificantBits() & 0xFFFFFFFF_FFFF0000L) | 0x0000000000004000L, 0x80000000_00000000L);
    }

    /**
     * Set the site id into the given object id
     * 
     * @param siteId
     * @param objectId
     * @return
     */
    public static UUID setSiteId(UUID siteId, UUID objectId)
    {
        return new UUID((siteId.getMostSignificantBits() & 0xFFFFFFFF_FFFF0000L) | (objectId.getMostSignificantBits() & 0x00000000_0000FFFFL), objectId.getLeastSignificantBits());
    }

    public static void main(String[] args)
    {
        UUID randId = UUID.randomUUID();
        UUID siteId = randomSiteId();
        UUID objectId = randomId(siteId);
        //
        System.out.println("Rand: " + randId + " " + randId.version() + " " + randId.variant());
        System.out.println("Site: " + siteId + " " + siteId.version() + " " + siteId.variant());
        System.out.println("Object: " + objectId + " " + siteId.version() + " " + siteId.variant());
        System.out.println("Site of Object: " + getSiteId(objectId) + " " + siteId.equals(getSiteId(objectId)));
    }
}