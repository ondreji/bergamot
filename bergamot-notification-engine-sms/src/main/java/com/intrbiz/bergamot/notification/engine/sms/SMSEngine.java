package com.intrbiz.bergamot.notification.engine.sms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.intrbiz.Util;
import com.intrbiz.bergamot.model.message.ContactMO;
import com.intrbiz.bergamot.model.message.notification.Notification;
import com.intrbiz.bergamot.notification.AbstractNotificationEngine;
import com.intrbiz.gerald.source.IntelligenceSource;
import com.intrbiz.gerald.witchcraft.Witchcraft;
import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.resource.factory.MessageFactory;
import com.twilio.sdk.resource.instance.Message;

public class SMSEngine extends AbstractNotificationEngine
{
    public static final String NAME = "sms";

    private Logger logger = Logger.getLogger(SMSEngine.class);

    private String accountSid;

    private String authToken;

    private String from;

    private TwilioRestClient client;

    private MessageFactory messageFactory;
    
    private final Timer smsSendTimer;
    
    private final Counter smsSendErrors;

    public SMSEngine()
    {
        super(NAME);
        // setup metrics
        IntelligenceSource source = Witchcraft.get().source("com.intrbiz.bergamot.sms");
        this.smsSendTimer = source.getRegistry().timer("sms-sent");
        this.smsSendErrors = source.getRegistry().counter("sms-errors");
    }

    @Override
    protected void configure() throws Exception
    {
        super.configure();
        // auth details
        this.accountSid = this.config.getStringParameterValue("twillo.account", "");
        this.authToken = this.config.getStringParameterValue("twillo.token", "");
        // from number
        this.from = this.config.getStringParameterValue("from", "");
        // setup the client
        logger.info("Using the Twillo account: " + this.accountSid + ", from: " + this.from);
        this.client = new TwilioRestClient(this.accountSid, this.authToken);
        this.messageFactory = client.getAccount().getMessageFactory();
    }

    @Override
    public void sendNotification(Notification notification)
    {
        logger.info("Sending SMS notification for " + notification.getCheck() + " to " + notification.getTo());
        Timer.Context tctx = this.smsSendTimer.time();
        try
        {
            try
            {
                if (!this.checkAtLeastOneRecipient(notification)) return;
                // build the message
                String message = this.buildMessage(notification);
                if (Util.isEmpty(message)) throw new RuntimeException("Failed to build message, not sending notifications");
                // send the SMSes
                for (ContactMO contact : notification.getTo())
                {
                    if ((!Util.isEmpty(contact.getPager())) && contact.hasEngine(this.getName()))
                    {
                        try
                        {
                            // send the SMS
                            List<NameValuePair> params = new ArrayList<NameValuePair>();
                            params.add(new BasicNameValuePair("To", contact.getPager()));
                            params.add(new BasicNameValuePair("From", this.from));
                            params.add(new BasicNameValuePair("Body", message));
                            Message sms = this.messageFactory.create(params);
                            logger.info("Sent SMS, Id: " + sms.getSid());
                            
                        }
                        catch (Exception e)
                        {
                            this.smsSendErrors.inc();
                            logger.error("Failed to send SMS notification to " + contact.getPager() + " - " + contact.getName());
                        }
                    }
                }
            }
            catch (Exception e)
            {
                this.smsSendErrors.inc();
                logger.error("Failed to send SMS notification", e);
            }
        }
        finally
        {
            tctx.stop();
        }
    }

    protected boolean checkAtLeastOneRecipient(Notification notification)
    {
        for (ContactMO contact : notification.getTo())
        {
            if ((!Util.isEmpty(contact.getPager())) && contact.hasEngine(this.getName())) { return true; }
        }
        return false;
    }

    protected String buildMessage(Notification notification) throws Exception
    {
        return this.applyTemplate(notification.getCheck().getCheckType() + "." + notification.getNotificationType() + ".message", notification);
    }

    @Override
    protected Map<String, String> getDefaultTemplates()
    {        
        Map<String, String> templates = new HashMap<String, String>();
        // host
        templates.put("host.alert.message", "Alert for host #{host.summary} is #{if(host.state.ok, 'UP', 'DOWN')}");
        templates.put("host.recovery.message", "Recovery for host #{host.summary} is #{if(host.state.ok, 'UP', 'DOWN')}");
        // cluster
        templates.put("cluster.alert.message", "Alert for cluster #{cluster.summary} is #{if(cluster.state.ok, 'UP', 'DOWN')}");
        templates.put("cluster.recovery.message", "Recovery for cluster #{cluster.summary} is #{if(cluster.state.ok, 'UP', 'DOWN')}");
        // service
        templates.put("service.alert.message", "Alert for service #{service.summary} on the host #{host.summary} is #{service.state.status}");
        templates.put("service.recovery.message", "Recovery for service #{service.summary} on the host #{host.summary} is #{service.state.status}");
        // trap
        templates.put("trap.alert.message", "Alert for trap #{trap.summary} on the host #{host.summary} is #{trap.state.status}");
        templates.put("trap.recovery.message", "Recovery for trap #{trap.summary} on the host #{host.summary} is #{trap.state.status}");
        // resource
        templates.put("resource.alert.message", "Alert for resource #{resource.summary} on the cluster #{cluster.summary} is #{resource.state.status}");
        templates.put("resource.recovery.message", "Recovery for resource #{resource.summary} on the cluster #{cluster.summary} is #{resource.state.status}");
        return templates;
    }
}
