package com.intrbiz.bergamot.health;

import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import com.intrbiz.bergamot.health.model.KnownDaemon;
import com.intrbiz.bergamot.model.message.health.HealthCheckJoin;
import com.intrbiz.bergamot.model.message.health.HealthCheckMessage;
import com.intrbiz.bergamot.model.message.health.HealthCheckUnjoin;
import com.intrbiz.bergamot.queue.HealthCheckQueue;
import com.intrbiz.queue.Producer;
import com.intrbiz.queue.name.NullKey;

public class HealthTracker
{
    private static final HealthTracker US = new HealthTracker();
    
    public static HealthTracker getInstance()
    {
        return US;
    }
    
    private ConcurrentMap<UUID, KnownDaemon> knownDaemons = new ConcurrentHashMap<UUID, KnownDaemon>();
    
    private volatile boolean inited = false;
    
    private HealthCheckQueue queue;
    
    @SuppressWarnings("unused")
    private com.intrbiz.queue.Consumer<HealthCheckMessage, NullKey> healthcheckConsumer;
    
    private Producer<HealthCheckMessage> healthcheckEventProducer;
    
    private static final Logger logger = Logger.getLogger(HealthTracker.class);
    
    private Timer timer;
    
    private final CopyOnWriteArrayList<Consumer<KnownDaemon>> alertHandlers = new CopyOnWriteArrayList<Consumer<KnownDaemon>>();
    
    private HealthTracker()
    {
        super();
    }
    
    public void init()
    {
        synchronized (this)
        {
            if (! this.inited)
            {
                this.inited = true;
                // setup queues
                this.setupQueue();
                // setup the check timer
                this.setupTimer();
            }
        }
    }
    
    public Set<KnownDaemon> getDaemons()
    {
        return new TreeSet<KnownDaemon>(this.knownDaemons.values());
    }
    
    public KnownDaemon getDaemon(UUID instanceId)
    {
        return this.knownDaemons.get(instanceId);
    }
    
    public void removeDaemon(UUID daemon)
    {
        this.knownDaemons.remove(daemon);
    }
    
    public void addAlertHandler(Consumer<KnownDaemon> alertHandler)
    {
        this.alertHandlers.add(alertHandler);
    }
    
    public void removeAlertHandler(Consumer<KnownDaemon> alertHandler)
    {
        this.alertHandlers.remove(alertHandler);
    }
    
    /**
     * Unjoin the given instance from this health check cluster
     * @param instanceId the instance id to unjoin
     * @param daemonKind the daemin kind
     * @param daemonName the daemon name
     */
    public void unjoinDaemon(UUID instanceId, String daemonKind, String daemonName)
    {
        this.healthcheckEventProducer.publish(new HealthCheckUnjoin(instanceId, daemonKind, daemonName));
    }
    
    /**
     * Unjoin the given instance from this health check cluster
     * @param instanceId the instance id to unjoin
     */
    public void unjoinDaemon(UUID instanceId)
    {
        this.unjoinDaemon(instanceId, null, "unknown");
    }
    
    private void setupTimer()
    {
        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run()
            {
                checkDaemons();
            }
        }, 
        11_000L, 5_000L);
    }
    
    private void checkDaemons()
    {
        for (KnownDaemon daemon : this.knownDaemons.values())
        {
            if (daemon.isAlive() && daemon.isLastHeartbeatTooOld())
            {
                // daemon was alive but we've missed enough heartbeats 
                // for it to now be considered dead
                daemon.setAlive(false);
                daemon.incAlertCount();
                daemon.setLastAlertTime(System.currentTimeMillis());
                // raise alert
                for (Consumer<KnownDaemon> handler : this.alertHandlers)
                {
                    handler.accept(daemon);
                }
            }
            else if ((! daemon.isAlive()) && daemon.isDaemonLongGone())
            {
                // remove the daemon from our list
                this.knownDaemons.remove(daemon.getInstanceId());
            }
        }
    }
    
    private void setupQueue()
    {
        this.queue = HealthCheckQueue.open();
        this.healthcheckEventProducer = this.queue.publishHealthCheckEvents();
        this.healthcheckConsumer = this.queue.consumeHealthCheckEvents(this::handleMessage);
    }
    
    private void handleMessage(Map<String, Object> headers, HealthCheckMessage message)
    {
        if (message instanceof HealthCheckJoin)
        {
            // join
            this.processJoin((HealthCheckJoin) message);
        }
        else if (message instanceof HealthCheckUnjoin)
        {
            // unjoin
            this.processUnjoin((HealthCheckUnjoin) message);
        }
    }
    
    private void processJoin(HealthCheckJoin join)
    {
        // lookup the daemon
        KnownDaemon daemon = this.knownDaemons.get(join.getInstanceId());
        if (daemon != null)
        {
            // use nanoTime as that is monotonic
            daemon.setLastHeartbeatAt(System.nanoTime());
            daemon.setLastHeartbeatTime(System.currentTimeMillis());
            // recovery handling
            if (! daemon.isAlive())
            {
                if (daemon.incRecoveryHeartbeatCount() >= 60)
                {
                    // we've have 5 minutes of successful heartbeats
                    daemon.incRecoveryCount();
                    daemon.setRecoveryHeartbeatCount(0);
                    daemon.setAlive(true);
                    daemon.setLastRecoveryTime(System.currentTimeMillis());
                }
            }
        }
        else
        {
            this.knownDaemons.put(join.getInstanceId(), new KnownDaemon(join.getInstanceId(), join.getRuntimeId(), join.getDaemonKind(), join.getDaemonName(), join.getStarted(), join.getHostId(), join.getHostName()));
            logger.info("Received join from " + join.getInstanceId() + " daemon " + join.getDaemonKind() + "::" + join.getDaemonName());
        }
    }
    
    private void processUnjoin(HealthCheckUnjoin unjoin)
    {
        this.knownDaemons.remove(unjoin.getInstanceId());
        logger.info("Received unjoin event from " + unjoin.getInstanceId() + " daemon " + unjoin.getDaemonName());
    }
}
