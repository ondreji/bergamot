package com.intrbiz.lamplighter.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.intrbiz.Util;
import com.intrbiz.bergamot.model.Site;
import com.intrbiz.data.DataManager;
import com.intrbiz.data.cache.Cache;
import com.intrbiz.data.cache.Cacheable;
import com.intrbiz.data.db.DatabaseAdapter;
import com.intrbiz.data.db.DatabaseConnection;
import com.intrbiz.data.db.compiler.DatabaseAdapterCompiler;
import com.intrbiz.data.db.compiler.meta.SQLCustom;
import com.intrbiz.data.db.compiler.meta.SQLGetter;
import com.intrbiz.data.db.compiler.meta.SQLOrder;
import com.intrbiz.data.db.compiler.meta.SQLParam;
import com.intrbiz.data.db.compiler.meta.SQLPatch;
import com.intrbiz.data.db.compiler.meta.SQLSchema;
import com.intrbiz.data.db.compiler.meta.SQLSetter;
import com.intrbiz.data.db.compiler.meta.SQLUserDefined;
import com.intrbiz.data.db.compiler.meta.SQLVersion;
import com.intrbiz.data.db.compiler.meta.ScriptType;
import com.intrbiz.data.db.compiler.util.SQLScript;
import com.intrbiz.lamplighter.model.CheckReading;
import com.intrbiz.lamplighter.model.StoredDoubleGaugeReading;
import com.intrbiz.lamplighter.model.StoredFloatGaugeReading;
import com.intrbiz.lamplighter.model.StoredIntGaugeReading;
import com.intrbiz.lamplighter.model.StoredLongGaugeReading;
import com.intrbiz.lamplighter.model.StoredMeterReading;
import com.intrbiz.lamplighter.model.StoredTimerReading;

@SQLSchema(
        name = "lamplighter", 
        version = @SQLVersion({1, 10, 0}),
        tables = {
            CheckReading.class,
            StoredDoubleGaugeReading.class,
            StoredLongGaugeReading.class,
            StoredIntGaugeReading.class,
            StoredFloatGaugeReading.class,
            StoredMeterReading.class,
            StoredTimerReading.class
        }
)
public abstract class LamplighterDB extends DatabaseAdapter
{

    /**
     * Compile and register the Bergamot Database Adapter
     */
    static
    {
        DataManager.getInstance().registerDatabaseAdapter(
                LamplighterDB.class, 
                DatabaseAdapterCompiler.defaultPGSQLCompiler().compileAdapterFactory(LamplighterDB.class)
        );
    }
    
    public static void load()
    {
        // do nothing
    }
    
    /**
     * Install the Bergamot schema into the default database
     */
    public static void install()
    {
        Logger logger = Logger.getLogger(LamplighterDB.class);
        DatabaseConnection database = DataManager.getInstance().connect();
        DatabaseAdapterCompiler compiler =  DatabaseAdapterCompiler.defaultPGSQLCompiler().setDefaultOwner("bergamot");
        // check if the schema is installed
        if (! compiler.isSchemaInstalled(database, LamplighterDB.class))
        {
            logger.info("Installing database schema");
            compiler.installSchema(database, LamplighterDB.class);
        }
        else
        {
            // check the installed schema is upto date
            if (! compiler.isSchemaUptoDate(database, LamplighterDB.class))
            {
                logger.info("The installed database schema is not upto date");
                compiler.upgradeSchema(database, LamplighterDB.class);
            }
            else
            {
                logger.info("The installed database schema is upto date");
            }
        }
    }

    /**
     * Connect to the Bergamot database
     */
    public static LamplighterDB connect()
    {
        return DataManager.getInstance().databaseAdapter(LamplighterDB.class);
    }
    
    /**
     * Connect to the Bergamot database
     */
    public static LamplighterDB connect(DatabaseConnection connection)
    {
        return DataManager.getInstance().databaseAdapter(LamplighterDB.class, connection);
    }

    public LamplighterDB(DatabaseConnection connection, Cache cache)
    {
        super(connection, cache);
    }
    
    public static void main(String[] args) throws Exception
    {
        if (args.length == 1 && "install".equals(args[0]))
        {
            DatabaseAdapterCompiler.main(new String[] { "install", LamplighterDB.class.getCanonicalName() });
        }
        else if (args.length == 2 && "upgrade".equals(args[0]))
        {
            DatabaseAdapterCompiler.main(new String[] { "upgrade", LamplighterDB.class.getCanonicalName(), args[1] });
        }
        else
        {
            // interactive
            try (Scanner input = new Scanner(System.in))
            {
                for (;;)
                {
                    System.out.print("Would you like to generate the install or upgrade schema: ");
                    String action = input.nextLine();
                    // process the action
                    if ("exit".equals(action) || "quit".equals(action) || "q".equals(action))
                    {
                        System.exit(0);
                    }
                    else if ("install".equalsIgnoreCase(action) || "in".equalsIgnoreCase(action) || "i".equalsIgnoreCase(action))
                    {
                        DatabaseAdapterCompiler.main(new String[] { "install", LamplighterDB.class.getCanonicalName() });
                        System.exit(0);
                    }
                    else if ("upgrade".equalsIgnoreCase(action) || "up".equalsIgnoreCase(action) || "u".equalsIgnoreCase(action))
                    {
                        System.out.print("What is the current installed version: ");
                        String version = input.nextLine();
                        DatabaseAdapterCompiler.main(new String[] { "upgrade", LamplighterDB.class.getCanonicalName(), version });
                        System.exit(0);
                    }
                }
            }
        }
    }
    
    // reading metadata
    
    @Cacheable
    @SQLGetter(table = CheckReading.class, name = "get_check_reading", since = @SQLVersion({1, 0, 0}))
    public abstract CheckReading getCheckReading(@SQLParam("id") UUID id);
    
    @Cacheable
    @SQLGetter(table = CheckReading.class, name ="get_check_reading_by_name", since = @SQLVersion({1, 0, 0}), orderBy = @SQLOrder("name"))
    public abstract CheckReading getCheckReadingByName(@SQLParam("check_id") UUID checkId, @SQLParam("name") String name);
    
    @Cacheable
    @SQLGetter(table = CheckReading.class, name ="get_check_readings_for_check", since = @SQLVersion({1, 0, 0}), orderBy = @SQLOrder("name"))
    public abstract List<CheckReading> getCheckReadingsForCheck(@SQLParam("check_id") UUID checkId);
    
    @SQLGetter(table = CheckReading.class, name = "list_check_readings", since = @SQLVersion({1, 0, 0}), orderBy = @SQLOrder("name"))
    public abstract List<CheckReading> listCheckReadings();
    
    // reading management
    
    @SQLCustom(
            since = @SQLVersion({1, 0, 0}),
            userDefined = @SQLUserDefined(resources = "new_site.sql")
    )
    public int setupSiteReadings(UUID siteId)
    {
        return this.use((with) -> {
            try (PreparedStatement stmt = with.prepareStatement("SELECT lamplighter.new_site(?::UUID)"))
            {
              stmt.setObject(1, siteId);
              try (ResultSet rs = stmt.executeQuery())
              {
                if (rs.next())
                {
                    return rs.getInt(1);
                }
              }
            }
            return null;
        });
    }
    
    @SQLCustom(
            since = @SQLVersion({1, 10, 0}),
            userDefined = @SQLUserDefined(resources = {
                    "create_double_gauge_reading.sql",
                    "create_long_gauge_reading.sql",
                    "create_int_gauge_reading.sql",
                    "create_float_gauge_reading.sql",
                    "create_meter_reading.sql",
                    "create_timer_reading.sql",
                    "new_reading.sql"
            })
    )
    public int newReading(UUID siteId, UUID readingId, UUID checkId, String name, String summary, String description, String unit, String readingType, long pollInterval)
    {
        int r = this.use((with) -> {
            try (PreparedStatement stmt = with.prepareStatement("SELECT lamplighter.new_reading(?, ?, ?, ?, ?, ?, ?, ?, ?)"))
            {
              stmt.setObject(1, siteId);
              stmt.setObject(2, readingId);
              stmt.setObject(3, checkId);
              stmt.setString(4, name);
              stmt.setString(5, summary);
              stmt.setString(6, description);
              stmt.setString(7, unit);
              stmt.setString(8, readingType);
              stmt.setLong(9, pollInterval);
              try (ResultSet rs = stmt.executeQuery())
              {
                if (rs.next())
                {
                    return rs.getInt(1);
                }
              }
            }
            return null;
        });
        // invalidate caches
        this.getAdapterCache().removePrefix("get_check_reading_by_name." + checkId);
        this.getAdapterCache().removePrefix("get_check_readings_for_check." + checkId);
        return r;
    }
    
    public CheckReading getOrSetupDoubleGaugeReading(UUID checkId, String name, String unit, long pollInterval)
    {
        // does it already exist
        CheckReading reading = this.getCheckReadingByName(checkId, name);
        if (reading == null)
        {
            UUID siteId = Site.getSiteId(checkId);
            UUID readingId = Site.randomId(siteId);
            // setup
            this.newReading(siteId, readingId, checkId, name, nameToSummary(name), null, unit, "double_gauge_reading", pollInterval);
            this.commit();
            // get the metadata
            reading = this.getCheckReading(readingId);
        }
        return reading;
    }
    
    public CheckReading getOrSetupLongGaugeReading(UUID checkId, String name, String unit, long pollInterval)
    {
        // does it already exist
        CheckReading reading = this.getCheckReadingByName(checkId, name);
        if (reading == null)
        {
            UUID siteId = Site.getSiteId(checkId);
            UUID readingId = Site.randomId(siteId);
            // setup
            this.newReading(siteId, readingId, checkId, name, nameToSummary(name), null, unit, "long_gauge_reading", pollInterval);
            this.commit();
            // get the metadata
            reading = this.getCheckReading(readingId);
        }
        return reading;
    }
    
    public CheckReading getOrSetupIntGaugeReading(UUID checkId, String name, String unit, long pollInterval)
    {
        // does it already exist
        CheckReading reading = this.getCheckReadingByName(checkId, name);
        if (reading == null)
        {
            UUID siteId = Site.getSiteId(checkId);
            UUID readingId = Site.randomId(siteId);
            // setup
            this.newReading(siteId, readingId, checkId, name, nameToSummary(name), null, unit, "int_gauge_reading", pollInterval);
            this.commit();
            // get the metadata
            reading = this.getCheckReading(readingId);
        }
        return reading;
    }
    
    public CheckReading getOrSetupFloatGaugeReading(UUID checkId, String name, String unit, long pollInterval)
    {
        // does it already exist
        CheckReading reading = this.getCheckReadingByName(checkId, name);
        if (reading == null)
        {
            UUID siteId = Site.getSiteId(checkId);
            UUID readingId = Site.randomId(siteId);
            // setup
            this.newReading(siteId, readingId, checkId, name, nameToSummary(name), null, unit, "float_gauge_reading", pollInterval);
            this.commit();
            // get the metadata
            reading = this.getCheckReading(readingId);
        }
        return reading;
    }
    
    public CheckReading getOrSetupMeterReading(UUID checkId, String name, String unit, long pollInterval)
    {
        // does it already exist
        CheckReading reading = this.getCheckReadingByName(checkId, name);
        if (reading == null)
        {
            UUID siteId = Site.getSiteId(checkId);
            UUID readingId = Site.randomId(siteId);
            // setup
            this.newReading(siteId, readingId, checkId, name, nameToSummary(name), null, unit, "meter_reading", pollInterval);
            this.commit();
            // get the metadata
            reading = this.getCheckReading(readingId);
        }
        return reading;
    }
    
    public CheckReading getOrSetupTimerReading(UUID checkId, String name, String unit, long pollInterval)
    {
        // does it already exist
        CheckReading reading = this.getCheckReadingByName(checkId, name);
        if (reading == null)
        {
            UUID siteId = Site.getSiteId(checkId);
            UUID readingId = Site.randomId(siteId);
            // setup
            this.newReading(siteId, readingId, checkId, name, nameToSummary(name), null, unit, "timer_reading", pollInterval);
            this.commit();
            // get the metadata
            reading = this.getCheckReading(readingId);
        }
        return reading;
    }
    
    // gauges
    
    // double
    
    @SQLSetter(
            table = StoredDoubleGaugeReading.class, name = "store_double_gauge_reading", upsert = false, 
            since = @SQLVersion({1, 0, 0}),
            userDefined = @SQLUserDefined(resources = "store_double_gauge_reading.sql")
    )
    public abstract void storeDoubleGaugeReading(StoredDoubleGaugeReading reading);
    
    @SQLGetter(
            table = StoredDoubleGaugeReading.class, name ="get_latest_double_gauge_readings", 
            since = @SQLVersion({1, 0, 0}),
            userDefined = @SQLUserDefined(resources = "get_latest_double_gauge_readings.sql")
    )
    public abstract List<StoredDoubleGaugeReading> getLatestDoubleGaugeReadings(@SQLParam("site_id") UUID siteId, @SQLParam("reading_id") UUID readingId, @SQLParam(value = "limit", virtual = true) int limit);
    
    @SQLGetter(
            table = StoredDoubleGaugeReading.class, name ="get_double_gauge_readings_by_date", 
            since = @SQLVersion({1, 0, 0}),
            userDefined = @SQLUserDefined(resources = "get_double_gauge_readings_by_date.sql")
    )
    public abstract List<StoredDoubleGaugeReading> getDoubleGaugeReadingsByDate(@SQLParam("site_id") UUID siteId, @SQLParam("reading_id") UUID readingId, @SQLParam(value = "start", virtual = true) Timestamp start, @SQLParam(value = "end", virtual = true) Timestamp end, @SQLParam(value = "rollup", virtual = true) long rollup, @SQLParam(value = "agg", virtual = true) String agg);
    
    // long
    
    @SQLSetter(
            table = StoredLongGaugeReading.class, name = "store_long_gauge_reading", upsert = false, 
            since = @SQLVersion({1, 0, 0}),
            userDefined = @SQLUserDefined(resources = "store_long_gauge_reading.sql")
    )
    public abstract void storeLongGaugeReading(StoredLongGaugeReading reading);
    
    @SQLGetter(
            table = StoredLongGaugeReading.class, name ="get_latest_long_gauge_readings", 
            since = @SQLVersion({1, 0, 0}),
            userDefined = @SQLUserDefined(resources = "get_latest_long_gauge_readings.sql")
    )
    public abstract List<StoredLongGaugeReading> getLatestLongGaugeReadings(@SQLParam("site_id") UUID siteId, @SQLParam("reading_id") UUID readingId, @SQLParam(value = "limit", virtual = true) int limit);
    
    @SQLGetter(
            table = StoredLongGaugeReading.class, name ="get_long_gauge_readings_by_date", 
            since = @SQLVersion({1, 0, 0}),
            userDefined = @SQLUserDefined(resources = "get_long_gauge_readings_by_date.sql")
    )
    public abstract List<StoredLongGaugeReading> getLongGaugeReadingsByDate(@SQLParam("site_id") UUID siteId, @SQLParam("reading_id") UUID readingId, @SQLParam(value = "start", virtual = true) Timestamp start, @SQLParam(value = "end", virtual = true) Timestamp end, @SQLParam(value = "rollup", virtual = true) long rollup, @SQLParam(value = "agg", virtual = true) String agg);
    
    // int
    
    @SQLSetter(
            table = StoredIntGaugeReading.class, name = "store_int_gauge_reading", upsert = false, 
            since = @SQLVersion({1, 0, 0}),
            userDefined = @SQLUserDefined(resources = "store_int_gauge_reading.sql")
    )
    public abstract void storeIntGaugeReading(StoredIntGaugeReading reading);
    
    @SQLGetter(
            table = StoredIntGaugeReading.class, name ="get_latest_int_gauge_readings", 
            since = @SQLVersion({1, 0, 0}),
            userDefined = @SQLUserDefined(resources = "get_latest_int_gauge_readings.sql")
    )
    public abstract List<StoredIntGaugeReading> getLatestIntGaugeReadings(@SQLParam("site_id") UUID siteId, @SQLParam("reading_id") UUID readingId, @SQLParam(value = "limit", virtual = true) int limit);
    
    @SQLGetter(
            table = StoredIntGaugeReading.class, name ="get_int_gauge_readings_by_date", 
            since = @SQLVersion({1, 0, 0}),
            userDefined = @SQLUserDefined(resources = "get_int_gauge_readings_by_date.sql")
    )
    public abstract List<StoredIntGaugeReading> getIntGaugeReadingsByDate(@SQLParam("site_id") UUID siteId, @SQLParam("reading_id") UUID readingId, @SQLParam(value = "start", virtual = true) Timestamp start, @SQLParam(value = "end", virtual = true) Timestamp end, @SQLParam(value = "rollup", virtual = true) long rollup, @SQLParam(value = "agg", virtual = true) String agg);
    
    // float
    
    @SQLSetter(
            table = StoredFloatGaugeReading.class, name = "store_float_gauge_reading", upsert = false, 
            since = @SQLVersion({1, 0, 0}),
            userDefined = @SQLUserDefined(resources = "store_float_gauge_reading.sql")
    )
    public abstract void storeFloatGaugeReading(StoredFloatGaugeReading reading);
    
    @SQLGetter(
            table = StoredFloatGaugeReading.class, name ="get_latest_float_gauge_readings", 
            since = @SQLVersion({1, 0, 0}),
            userDefined = @SQLUserDefined(resources = "get_latest_float_gauge_readings.sql")
    )
    public abstract List<StoredFloatGaugeReading> getLatestFloatGaugeReadings(@SQLParam("site_id") UUID siteId, @SQLParam("reading_id") UUID readingId, @SQLParam(value = "limit", virtual = true) int limit);
    
    @SQLGetter(
            table = StoredFloatGaugeReading.class, name ="get_float_gauge_readings_by_date", 
            since = @SQLVersion({1, 0, 0}),
            userDefined = @SQLUserDefined(resources = "get_float_gauge_readings_by_date.sql")
    )
    public abstract List<StoredFloatGaugeReading> getFloatGaugeReadingsByDate(@SQLParam("site_id") UUID siteId, @SQLParam("reading_id") UUID readingId, @SQLParam(value = "start", virtual = true) Timestamp start, @SQLParam(value = "end", virtual = true) Timestamp end, @SQLParam(value = "rollup", virtual = true) long rollup, @SQLParam(value = "agg", virtual = true) String agg);
    
    // meter

    @SQLSetter(
            table = StoredMeterReading.class, name = "store_meter_reading", upsert = false, 
            since = @SQLVersion({1, 10, 0}),
            userDefined = @SQLUserDefined(resources = "store_meter_reading.sql")
    )
    public abstract void storeMeterReading(StoredMeterReading reading);
    
    @SQLGetter(
            table = StoredMeterReading.class, name ="get_latest_meter_readings", 
            since = @SQLVersion({1, 10, 0}),
            userDefined = @SQLUserDefined(resources = "get_latest_meter_readings.sql")
    )
    public abstract List<StoredMeterReading> getLatestMeterReadings(@SQLParam("site_id") UUID siteId, @SQLParam("reading_id") UUID readingId, @SQLParam(value = "limit", virtual = true) int limit);
    
    @SQLGetter(
            table = StoredMeterReading.class, name ="get_meter_readings_by_date", 
            since = @SQLVersion({1, 10, 0}),
            userDefined = @SQLUserDefined(resources = "get_meter_readings_by_date.sql")
    )
    public abstract List<StoredMeterReading> getMeterReadingsByDate(@SQLParam("site_id") UUID siteId, @SQLParam("reading_id") UUID readingId, @SQLParam(value = "start", virtual = true) Timestamp start, @SQLParam(value = "end", virtual = true) Timestamp end, @SQLParam(value = "rollup", virtual = true) long rollup, @SQLParam(value = "agg", virtual = true) String agg);
    
    // timer
   
    @SQLSetter(
            table = StoredTimerReading.class, name = "store_timer_reading", upsert = false, 
            since = @SQLVersion({1, 10, 0}),
            userDefined = @SQLUserDefined(resources = "store_timer_reading.sql")
    )
    public abstract void storeTimerReading(StoredTimerReading reading);
    
    @SQLGetter(
            table = StoredTimerReading.class, name ="get_latest_timer_readings", 
            since = @SQLVersion({1, 10, 0}),
            userDefined = @SQLUserDefined(resources = "get_latest_timer_readings.sql")
    )
    public abstract List<StoredTimerReading> getLatestTimerReadings(@SQLParam("site_id") UUID siteId, @SQLParam("reading_id") UUID readingId, @SQLParam(value = "limit", virtual = true) int limit);
    
    @SQLGetter(
            table = StoredTimerReading.class, name ="get_timer_readings_by_date", 
            since = @SQLVersion({1, 10, 0}),
            userDefined = @SQLUserDefined(resources = "get_timer_readings_by_date.sql")
    )
    public abstract List<StoredTimerReading> getTimerReadingsByDate(@SQLParam("site_id") UUID siteId, @SQLParam("reading_id") UUID readingId, @SQLParam(value = "start", virtual = true) Timestamp start, @SQLParam(value = "end", virtual = true) Timestamp end, @SQLParam(value = "rollup", virtual = true) long rollup, @SQLParam(value = "agg", virtual = true) String agg);
    
    // custom helper functions
    
    @SQLCustom(
            since = @SQLVersion({1, 0, 0}),
            userDefined = @SQLUserDefined("CREATE OR REPLACE FUNCTION lamplighter.get_default_owner() RETURNS TEXT LANGUAGE SQL AS $body$ SELECT 'bergamot'::TEXT; $body$;")
    )
    public String getDefaultOwner()
    {
        return this.use((con) -> {
            try (PreparedStatement stmt = con.prepareStatement("SELECT lamplighter.get_default_owner()"))
            {
                try (ResultSet rs = stmt.executeQuery())
                {
                    if (rs.next()) return rs.getString(1);
                }
            }
            return null;
        });
    }
    
    @SQLCustom(
            since = @SQLVersion({1, 0, 0}),
            userDefined = @SQLUserDefined("CREATE OR REPLACE FUNCTION lamplighter.get_schema(p_site_id UUID) RETURNS TEXT LANGUAGE SQL AS $body$ SELECT ('readings_' || $1)::TEXT; $body$;")
    )
    public String getSchema(UUID siteId)
    {
        return this.use((con) -> {
            try (PreparedStatement stmt = con.prepareStatement("SELECT lamplighter.get_schema(?::UUID)"))
            {
                stmt.setString(1, siteId == null ? null : siteId.toString());
                try (ResultSet rs = stmt.executeQuery())
                {
                    if (rs.next()) return rs.getString(1);
                }
            }
            return null;
        });
    }
    
    @SQLCustom(
            since = @SQLVersion({1, 0, 0}),
            userDefined = @SQLUserDefined("CREATE OR REPLACE FUNCTION lamplighter.get_table_name(p_type TEXT, p_reading_id UUID) RETURNS TEXT LANGUAGE sql AS $body$ SELECT (CASE WHEN ($2 IS NULL) THEN $1 ELSE 'reading_' || p_reading_id END)::TEXT $body$;")
    )
    public String getTableName(String type, UUID siteId, UUID readingId)
    {
        return this.use((con) -> {
            try (PreparedStatement stmt = con.prepareStatement("SELECT lamplighter.get_schema(?::UUID)"))
            {
                stmt.setString(1, type);
                stmt.setString(2, siteId == null ? null : siteId.toString());
                stmt.setString(3, readingId == null ? null : readingId.toString());
                try (ResultSet rs = stmt.executeQuery())
                {
                    if (rs.next()) return rs.getString(1);
                }
            }
            return null;
        });
    }
    
    @SQLCustom(
            since = @SQLVersion({1, 0, 0}),
            userDefined = @SQLUserDefined(resources = "round_time.sql")
    )
    public Timestamp roundTime(Timestamp time, long round)
    {
        return this.use((con) -> {
            try (PreparedStatement stmt = con.prepareStatement("SELECT lamplighter.round_time(?, ?)"))
            {
                stmt.setTimestamp(1, time);
                stmt.setLong(2, round);
                try (ResultSet rs = stmt.executeQuery())
                {
                    if (rs.next()) return rs.getTimestamp(1);
                }
            }
            return null;
        });
    }
    
    @SQLPatch(name = "set_function_owner", index = 1000, type = ScriptType.BOTH_LAST, version = @SQLVersion({1, 10, 0}))
    protected static SQLScript setFunctionOWner()
    {
        return new SQLScript(
                // helpers
                "ALTER FUNCTION lamplighter.get_default_owner() OWNER TO bergamot;",
                "ALTER FUNCTION lamplighter.get_schema(UUID) OWNER TO bergamot;",
                "ALTER FUNCTION lamplighter.get_table_name(TEXT, UUID) OWNER TO bergamot;",
                "ALTER FUNCTION lamplighter.round_time(TIMESTAMP WITH TIME ZONE, BIGINT) OWNER TO bergamot;",
                // setup
                "ALTER FUNCTION lamplighter.new_site(UUID) OWNER TO bergamot;",
                "ALTER FUNCTION lamplighter.new_reading(UUID, UUID, UUID, TEXT, TEXT, TEXT, TEXT, TEXT, BIGINT) OWNER TO bergamot;",
                // double
                "ALTER FUNCTION lamplighter.create_double_gauge_reading(UUID, UUID) OWNER TO bergamot;",
                "ALTER FUNCTION lamplighter.store_double_gauge_reading(UUID, UUID, TIMESTAMP WITH TIME ZONE, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION) OWNER TO bergamot;",
                "ALTER FUNCTION lamplighter.get_latest_double_gauge_readings(UUID, UUID, INTEGER) OWNER TO bergamot;",
                "ALTER FUNCTION lamplighter.get_double_gauge_readings_by_date(UUID, UUID, TIMESTAMP WITH TIME ZONE, TIMESTAMP WITH TIME ZONE, BIGINT, TEXT) OWNER TO bergamot;",
                // long
                "ALTER FUNCTION lamplighter.create_long_gauge_reading(UUID, UUID) OWNER TO bergamot;",
                "ALTER FUNCTION lamplighter.store_long_gauge_reading(UUID, UUID, TIMESTAMP WITH TIME ZONE, BIGINT, BIGINT, BIGINT, BIGINT, BIGINT) OWNER TO bergamot;",
                "ALTER FUNCTION lamplighter.get_latest_long_gauge_readings(UUID, UUID, INTEGER) OWNER TO bergamot;",
                "ALTER FUNCTION lamplighter.get_long_gauge_readings_by_date(UUID, UUID, TIMESTAMP WITH TIME ZONE, TIMESTAMP WITH TIME ZONE, BIGINT, TEXT) OWNER TO bergamot;",
                // int
                "ALTER FUNCTION lamplighter.create_int_gauge_reading(UUID, UUID) OWNER TO bergamot;",
                "ALTER FUNCTION lamplighter.store_int_gauge_reading(UUID, UUID, TIMESTAMP WITH TIME ZONE, INTEGER, INTEGER, INTEGER, INTEGER, INTEGER) OWNER TO bergamot;",
                "ALTER FUNCTION lamplighter.get_latest_int_gauge_readings(UUID, UUID, INTEGER) OWNER TO bergamot;",
                "ALTER FUNCTION lamplighter.get_int_gauge_readings_by_date(UUID, UUID, TIMESTAMP WITH TIME ZONE, TIMESTAMP WITH TIME ZONE, BIGINT, TEXT) OWNER TO bergamot;",
                // float
                "ALTER FUNCTION lamplighter.create_float_gauge_reading(UUID, UUID) OWNER TO bergamot;",
                "ALTER FUNCTION lamplighter.store_float_gauge_reading(UUID, UUID, TIMESTAMP WITH TIME ZONE, REAL, REAL, REAL, REAL, REAL) OWNER TO bergamot;",
                "ALTER FUNCTION lamplighter.get_latest_float_gauge_readings(UUID, UUID, INTEGER) OWNER TO bergamot;",
                "ALTER FUNCTION lamplighter.get_float_gauge_readings_by_date(UUID, UUID, TIMESTAMP WITH TIME ZONE, TIMESTAMP WITH TIME ZONE, BIGINT, TEXT) OWNER TO bergamot;",
                // meter
                "ALTER FUNCTION lamplighter.create_meter_reading(UUID, UUID) OWNER TO bergamot;",
                "ALTER FUNCTION lamplighter.store_meter_reading(UUID, UUID, TIMESTAMP WITH TIME ZONE, BIGINT, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION) OWNER TO bergamot;",
                "ALTER FUNCTION lamplighter.get_latest_meter_readings(UUID, UUID, INTEGER) OWNER TO bergamot;",
                "ALTER FUNCTION lamplighter.get_meter_readings_by_date(UUID, UUID, TIMESTAMP WITH TIME ZONE, TIMESTAMP WITH TIME ZONE, BIGINT, TEXT) OWNER TO bergamot;",
                // timer
                "ALTER FUNCTION lamplighter.create_timer_reading(UUID, UUID) OWNER TO bergamot;",
                "ALTER FUNCTION lamplighter.store_timer_reading(UUID, UUID, TIMESTAMP WITH TIME ZONE, BIGINT, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION) OWNER TO bergamot;",
                "ALTER FUNCTION lamplighter.get_latest_timer_readings(UUID, UUID, INTEGER) OWNER TO bergamot;",
                "ALTER FUNCTION lamplighter.get_timer_readings_by_date(UUID, UUID, TIMESTAMP WITH TIME ZONE, TIMESTAMP WITH TIME ZONE, BIGINT, TEXT) OWNER TO bergamot;"
        );
    }
    
    @SQLPatch(name = "setup_existing_sites", index = 2000, type = ScriptType.INSTALL_LAST, version = @SQLVersion({1, 0, 0}))
    protected static SQLScript setupExistingSites()
    {
        return new SQLScript(
                "SELECT lamplighter.new_site(id) FROM bergamot.site"
        );
    }
    
    @SQLPatch(name = "add_meter_and_timer_base_tables_to_sites", index = 2000, type = ScriptType.UPGRADE_LAST, version = @SQLVersion({1, 10, 0}))
    protected static SQLScript addMeterAndTimerBaseTablesToSites()
    {
        return new SQLScript(
                "SELECT lamplighter.create_meter_reading(id, null), lamplighter.create_timer_reading(id, null) FROM bergamot.site"
        );
    }
    
    private static String nameToSummary(String name)
    {
        return Util.ucFirst(name.replace('-', ' ').replace('_', ' ').replace('.', ' '));
    }
}
