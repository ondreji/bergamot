package com.intrbiz.bergamot.health;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.intrbiz.Util;
import com.intrbiz.bergamot.model.message.health.HealthCheckHeartbeat;
import com.intrbiz.bergamot.model.message.health.HealthCheckJoin;
import com.intrbiz.bergamot.model.message.health.HealthCheckKill;
import com.intrbiz.bergamot.model.message.health.HealthCheckMessage;
import com.intrbiz.bergamot.model.message.health.HealthCheckRequestJoin;
import com.intrbiz.bergamot.model.message.health.HealthCheckUnjoin;
import com.intrbiz.bergamot.queue.HealthCheckQueue;
import com.intrbiz.gerald.polyakov.Node;
import com.intrbiz.queue.Consumer;
import com.intrbiz.queue.Producer;
import com.intrbiz.queue.name.NullKey;

public final class HealthAgent
{
    private static final Logger logger = Logger.getLogger(HealthAgent.class);
    
    private static final HealthAgent US = new HealthAgent();
    
    public static HealthAgent getInstance()
    {
        return US;
    }
    
    private UUID instanceId = null;
    
    private final UUID runtimeId = UUID.randomUUID();
    
    private String daemonName;
    
    private final AtomicLong heartbeatSequence = new AtomicLong();
    
    private volatile boolean inited = false;
    
    private long startedAt;
    
    private UUID hostId;
    
    private String hostName;
    
    private String daemonKind;
    
    private HealthCheckQueue queue;
    
    private Producer<HealthCheckMessage> healthcheckProducer;
    
    @SuppressWarnings("unused")
    private Consumer<HealthCheckMessage, NullKey> healthcheckConsumer;
    
    private Timer timer;
    
    private HealthAgent()
    {
        super();
    }
    
    public void init(UUID instanceId, String daemonKind, String daemonName, UUID hostId, String hostName)
    {
        synchronized (this)
        {
            if (! this.inited)
            {
                this.inited = true;
                this.instanceId = instanceId;
                this.daemonKind = daemonKind;
                this.daemonName = daemonName;
                this.hostId = hostId;
                this.hostName = hostName;
                this.startedAt = System.currentTimeMillis();
                // setup our queues
                this.setupQueues();
                // setup shutdown hook
                this.setupShutdownHook();
                // send join
                this.joinHealthCheckCluster();
                // setup timer
                this.setupTimer();
            }
        }
    }
    
    public void init(String daemonKind, String daemonName)
    {
        Node node = Node.service(daemonName);
        this.init(computeInstanceId(), daemonKind, daemonName, node.getHostId(), node.getHostName());
    }
    
    public void init()
    {
        Node node = Node.service();
        this.init(computeInstanceId(), null, node.getServiceName(), node.getHostId(), node.getHostName());
    }
    
    public String getDaemonKind()
    {
        return this.daemonKind;
    }
    
    public String getDaemonName()
    {
        return this.daemonName;
    }
    
    public UUID getInstanceId()
    {
        return this.instanceId;
    }
    
    public UUID getRuntimeId()
    {
        return this.runtimeId;
    }
    
    public String getHostName()
    {
        return this.hostName;
    }
    
    public long getStartedAt()
    {
        return this.startedAt;
    }
    
    long nextHeartbeatSequence()
    {
        return this.heartbeatSequence.getAndIncrement();
    }
    
    private void setupQueues()
    {
        this.queue = HealthCheckQueue.open();
        this.healthcheckProducer = this.queue.publishHealthCheckEvents();
        this.healthcheckConsumer = this.queue.consumeHealthCheckControlEvents(this::handleHealthCheckMessage);
    }
    
    private void setupTimer()
    {
        this.timer = new Timer();
        // heartbeat every 5 seconds
        this.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run()
            {
                try
                {
                    sendHeartbeat();
                }
                catch (Exception e)
                {
                    // ignore
                }
            }
        }, 
        5_000L, 5_000L);
    }
    
    private void setupShutdownHook()
    {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run()
            {
                unjoinHealthCheckCluster();
            }
        });
    }
    
    private void joinHealthCheckCluster()
    {
        try
        {
            // send a join message
            this.healthcheckProducer.publish(new HealthCheckJoin(this.instanceId, this.runtimeId, this.daemonKind, this.daemonName, this.startedAt, this.hostId, this.hostName));
        }
        catch (Exception e)
        {
            logger.warn("Failed to join health check");
        }
    }
    
    private void unjoinHealthCheckCluster()
    {
        // send a join message
        this.healthcheckProducer.publish(new HealthCheckUnjoin(this.instanceId, this.daemonKind, this.daemonName));
    }
    
    private void sendHeartbeat()
    {
        this.healthcheckProducer.publish(new HealthCheckHeartbeat(this.instanceId, System.currentTimeMillis(), this.nextHeartbeatSequence()));
    }
    
    private void handleHealthCheckMessage(Map<String, Object> headers, HealthCheckMessage message)
    {
        if (message instanceof HealthCheckRequestJoin)
        {
            this.joinHealthCheckCluster();
        }
        else if (message instanceof HealthCheckKill)
        {
            this.commitSeppuku((HealthCheckKill) message);
        }
    }
    
    private void commitSeppuku(HealthCheckKill message)
    {
        if (this.instanceId.equals(message.getInstanceId()) && this.runtimeId.equals(message.getRuntimeId()))
        {
            String password = Util.coalesceEmpty(System.getProperty("bergamot.health.password"), System.getenv("bergamot.health.password"));
            if ((! Util.isEmpty(password)) && message.getPassword() != null && message.getPassword().equals(password))
            {
                Logger.getLogger(HealthAgent.class).fatal("Received request to terminate via health check, exiting immediately");
                System.err.println("Received request to terminate via health check, exiting immediately");
                System.exit(-1);
            }
        }
    }
    
    public static final UUID computeInstanceId()
    {
        // Attempt to find a configured specific instance id
        String instanceIdStr = System.getProperty("bergamot.instanceid", System.getenv("BERGAMOT_INSTANCEID"));
        if (instanceIdStr != null && instanceIdStr.length() > 0)
        {
            try
            {
                return UUID.fromString(instanceIdStr);
            }
            catch (IllegalArgumentException e)
            {
            }
        }
        Logger.getLogger(HealthAgent.class).warn("Failed to get service instance id, defaulting to a randomly allocated id, please set the environment variable \"BERGAMOT_INSTANCEID\" or the system property \"bergamot.instanceid\"");
        return UUID.randomUUID();
    }
}
