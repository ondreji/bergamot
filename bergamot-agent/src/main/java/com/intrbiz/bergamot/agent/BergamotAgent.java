package com.intrbiz.bergamot.agent;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.File;
import java.net.URI;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.intrbiz.bergamot.agent.handler.CPUInfoHandler;
import com.intrbiz.bergamot.agent.handler.DefaultHandler;
import com.intrbiz.bergamot.agent.handler.DiskInfoHandler;
import com.intrbiz.bergamot.agent.handler.MemInfoHandler;
import com.intrbiz.bergamot.agent.handler.NetIfInfoHandler;
import com.intrbiz.bergamot.agent.handler.OSInfoHandler;
import com.intrbiz.bergamot.agent.handler.UptimeInfoHandler;
import com.intrbiz.bergamot.crypto.util.KeyStoreUtil;
import com.intrbiz.bergamot.model.message.agent.AgentMessage;
import com.intrbiz.bergamot.model.message.agent.error.AgentError;
import com.intrbiz.bergamot.model.message.agent.ping.AgentPing;
import com.intrbiz.bergamot.model.message.agent.ping.AgentPong;
import com.intrbiz.gerald.polyakov.Node;
import com.intrbiz.util.IBThreadFactory;

/**
 */
public class BergamotAgent
{
    private Logger logger = Logger.getLogger(BergamotAgent.class);
    
    private URI server;

    private EventLoopGroup eventLoop;
    
    private Timer timer;
    
    private Node node;
    
    private ConcurrentMap<Class<?>, AgentHandler> handlers = new ConcurrentHashMap<Class<?>, AgentHandler>();
    
    private AgentHandler defaultHandler;
    
    private SSLContext sslContext;

    public BergamotAgent(URI server, Node node)
    {
        this.server = server;
        this.node = node;
        this.eventLoop = new NioEventLoopGroup(1, new IBThreadFactory("bergamot-agent", false));
        this.sslContext = this.createContext();
        this.timer = new Timer();
        // background GC task
        // we want to deliberately 
        // keep memory to a minimum
        this.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run()
            {
                System.gc();
                Runtime rt = Runtime.getRuntime();
                logger.debug("Memory: " + rt.freeMemory() + " " + rt.totalMemory() + " " + rt.maxMemory());
            }
        }, 30_000L, 30_000L);
        // handlers
        this.setDefaultHandler(new DefaultHandler());
        this.registerHandler(new CPUInfoHandler());
        this.registerHandler(new MemInfoHandler());
        this.registerHandler(new DiskInfoHandler());
        this.registerHandler(new OSInfoHandler());
        this.registerHandler(new UptimeInfoHandler());
        this.registerHandler(new NetIfInfoHandler());
    }
    
    private SSLContext createContext()
    {
        try
        {
            String pass = "abc123";
            // create the keystore
            KeyStore sks = KeyStoreUtil.loadClientAuthKeyStore(pass, new File("test.agent.key"), new File("test.agent.crt"), new File("ca.crt"));
            KeyStore tks = KeyStoreUtil.loadTrustKeyStore(new File("ca.crt"));
            // the key manager
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(sks, pass.toCharArray());
            // the trust manager
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(tks);
            // the context
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
            return context;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to init SSLContext");
        }
    }
    
    private SSLEngine createSSLEngine(String host, int port)
    {
        try
        {
            SSLEngine sslEngine = this.sslContext.createSSLEngine(host, port);
            sslEngine.setUseClientMode(true);
            sslEngine.setNeedClientAuth(true);
            SSLParameters params = new SSLParameters();
            params.setEndpointIdentificationAlgorithm("HTTPS");
            params.setNeedClientAuth(true);
            sslEngine.setSSLParameters(params);
            return sslEngine;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to init SSLEngine", e);
        }
    }
    
    public Node getNode()
    {
        return this.node;
    }
    
    public URI getServer()
    {
        return this.server;
    }
    
    public void registerHandler(AgentHandler handler)
    {
        for (Class<?> cls : handler.getMessages())
        {
            this.handlers.put(cls, handler);
        }
    }
    
    public void setDefaultHandler(AgentHandler handler)
    {
        this.defaultHandler = handler;
    }
    
    public AgentHandler getHandler(Class<?> messageType)
    {
        AgentHandler handler = this.handlers.get(messageType);
        return handler == null ? this.defaultHandler : handler;
    }

    public void connect() throws Exception
    {
        SSLEngine engine = createSSLEngine(this.server.getHost(), this.server.getPort());
        // configure the client
        Bootstrap b = new Bootstrap();
        b.group(this.eventLoop);
        b.channel(NioSocketChannel.class);
        b.handler(new ChannelInitializer<SocketChannel>()
        {
            @Override
            public void initChannel(SocketChannel ch) throws Exception
            {
                // HTTP handling
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast("read-timeout",  new ReadTimeoutHandler(  30 /* seconds */ )); 
                pipeline.addLast("write-timeout", new WriteTimeoutHandler( 30 /* seconds */ ));
                pipeline.addLast("ssl",           new SslHandler(engine));
                pipeline.addLast("codec",         new HttpClientCodec()); 
                pipeline.addLast("aggregator",    new HttpObjectAggregator(65536));
                pipeline.addLast("handler",       new AgentClientHandler(BergamotAgent.this.timer, BergamotAgent.this.server, BergamotAgent.this.node)
                {
                    @Override
                    protected AgentMessage processMessage(final ChannelHandlerContext ctx, final AgentMessage request)
                    {
                        if (request instanceof AgentPing)
                        {
                            logger.debug("Got ping from server");
                            return new AgentPong(UUID.randomUUID().toString());
                        }
                        else if (request instanceof AgentPong)
                        {
                            logger.debug("Got pong from server");
                            return null;
                        }
                        else if (request instanceof AgentError)
                        {
                            logger.warn("Got error from server: " + ((AgentError) request).getMessage());
                            return null;
                        }
                        else if (request != null)
                        {
                            AgentHandler handler = getHandler(request.getClass());
                            if (handler != null)
                            {
                                return handler.handle(request);
                            }
                        }
                        return null;
                    }
                });
            }
        });
        // connect the client
        b.connect(this.server.getHost(), this.server.getPort()).addListener(new GenericFutureListener<ChannelFuture>() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception
            {
                final Channel channel = future.channel();
                if (future.isDone() && future.isSuccess())
                {
                    // setup close listener
                    channel.closeFuture().addListener(new GenericFutureListener<ChannelFuture>() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception
                        {
                            BergamotAgent.this.scheduleReconnect();
                        }
                    });
                }
                else
                {
                    // schedule reconnect
                    BergamotAgent.this.scheduleReconnect();
                }
            }
        });        
    }
    
    protected void scheduleReconnect()
    {
        this.logger.info("Scheduling reconnection shortly");
        this.timer.schedule(new TimerTask() {
            @Override
            public void run()
            {
                try
                {
                    BergamotAgent.this.connect();
                }
                catch (Exception e)
                {
                    BergamotAgent.this.logger.error("Error connecting to server", e);
                    BergamotAgent.this.scheduleReconnect();
                }
            }
        }, 15_000L);
    }

    public void shutdown()
    {
        try
        {
            this.eventLoop.shutdownGracefully().await();
        }
        catch (InterruptedException e)
        {
        }
    }

    public static void main(String[] args) throws Exception
    {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.TRACE);
        //
        BergamotAgent agent = new BergamotAgent(new URI("ws://hub.bergamot.local:8081/agent"), Node.service("BergamotAgent"));
        agent.connect();
    }
}