package com.intrbiz.bergamot.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.intrbiz.bergamot.data.BergamotDB;
import com.intrbiz.bergamot.model.message.AlertMO;
import com.intrbiz.bergamot.model.message.ContactMO;
import com.intrbiz.bergamot.model.message.notification.AlertNotification;
import com.intrbiz.bergamot.model.message.notification.SendAcknowledge;
import com.intrbiz.bergamot.model.message.notification.SendAlert;
import com.intrbiz.bergamot.model.message.notification.SendRecovery;
import com.intrbiz.bergamot.model.state.CheckState;
import com.intrbiz.data.db.compiler.meta.Action;
import com.intrbiz.data.db.compiler.meta.SQLColumn;
import com.intrbiz.data.db.compiler.meta.SQLForeignKey;
import com.intrbiz.data.db.compiler.meta.SQLPrimaryKey;
import com.intrbiz.data.db.compiler.meta.SQLTable;
import com.intrbiz.data.db.compiler.meta.SQLVersion;

/**
 * An alert which was raised against a check
 */
@SQLTable(schema = BergamotDB.class, name = "alert", since = @SQLVersion({ 1, 0, 0 }))
public class Alert extends BergamotObject<AlertMO> implements Serializable, Commented
{
    private static final long serialVersionUID = 1L;

    /**
     * The unique ID for this alert
     */
    @SQLColumn(index = 1, name = "id", since = @SQLVersion({ 1, 0, 0 }))
    @SQLPrimaryKey()
    private UUID id;

    @SQLColumn(index = 2, name = "site_id", notNull = true, since = @SQLVersion({ 1, 0, 0 }))
    @SQLForeignKey(references = Site.class, on = "id", onDelete = Action.CASCADE, onUpdate = Action.RESTRICT, since = @SQLVersion({ 1, 0, 0 }))
    private UUID siteId;

    /**
     * The check to which this alert was issued
     */
    @SQLColumn(index = 3, name = "check_id", since = @SQLVersion({ 1, 0, 0 }))
    private UUID checkId;

    /**
     * When was this alert raised
     */
    @SQLColumn(index = 4, name = "raised", since = @SQLVersion({ 1, 0, 0 }))
    private Timestamp raised;

    /**
     * Is the check ok?
     */
    @SQLColumn(index = 5, name = "ok", since = @SQLVersion({ 1, 0, 0 }))
    private boolean ok = true;

    /**
     * Why is the check ok or not ok?
     */
    @SQLColumn(index = 6, name = "status", since = @SQLVersion({ 1, 0, 0 }))
    private Status status = Status.PENDING;

    /**
     * What was the output of the last check
     */
    @SQLColumn(index = 7, name = "output", since = @SQLVersion({ 1, 0, 0 }))
    private String output = "Pending";

    /**
     * When did the last check happen
     */
    @SQLColumn(index = 8, name = "last_check_time", since = @SQLVersion({ 1, 0, 0 }))
    private Timestamp lastCheckTime = new Timestamp(System.currentTimeMillis());

    /**
     * What was the Id of the last check
     */
    @SQLColumn(index = 9, name = "last_check_id", since = @SQLVersion({ 1, 0, 0 }))
    private UUID lastCheckId;

    /**
     * The number of attempts since the last hard state change
     */
    @SQLColumn(index = 10, name = "attempt", since = @SQLVersion({ 1, 0, 0 }))
    private int attempt = Integer.MAX_VALUE;

    /**
     * Has a hard state transition happened
     */
    @SQLColumn(index = 11, name = "hard", since = @SQLVersion({ 1, 0, 0 }))
    private boolean hard = true;

    /**
     * Is the state in transition
     */
    @SQLColumn(index = 12, name = "transitioning", since = @SQLVersion({ 1, 0, 0 }))
    private boolean transitioning = false;

    /**
     * Is the state flapping between ok and not ok, but never reaching a hard state
     */
    @SQLColumn(index = 13, name = "flapping", since = @SQLVersion({ 1, 0, 0 }))
    private boolean flapping = false;

    /**
     * When was the last hard state change
     */
    @SQLColumn(index = 14, name = "last_state_change", since = @SQLVersion({ 1, 0, 0 }))
    private Timestamp lastStateChange = new Timestamp(System.currentTimeMillis());

    // history

    /**
     * A bitmap of the ok history
     */
    @SQLColumn(index = 15, name = "ok_history", since = @SQLVersion({ 1, 0, 0 }))
    private long okHistory = 0x1L;

    /**
     * Was the last hard state ok?
     */
    @SQLColumn(index = 16, name = "last_hard_ok", since = @SQLVersion({ 1, 0, 0 }))
    private boolean lastHardOk = true;

    /**
     * What was the last hard status?
     */
    @SQLColumn(index = 17, name = "last_hard_status", since = @SQLVersion({ 1, 0, 0 }))
    private Status lastHardStatus = Status.PENDING;

    /**
     * What was the output of the last hard state
     */
    @SQLColumn(index = 18, name = "last_hard_output", since = @SQLVersion({ 1, 0, 0 }))
    private String lastHardOutput = "Pending";

    /**
     * Has this alert been acknowledged by somebody
     */
    @SQLColumn(index = 19, name = "acknowledged", since = @SQLVersion({ 1, 0, 0 }))
    private boolean acknowledged = false;

    /**
     * When was this alert acknowledged
     */
    @SQLColumn(index = 20, name = "acknowledged_at", since = @SQLVersion({ 1, 0, 0 }))
    private Timestamp acknowledgedAt;

    /**
     * Whom acknowledged this alert
     */
    @SQLColumn(index = 21, name = "acknowledged_by_id", since = @SQLVersion({ 1, 0, 0 }))
    @SQLForeignKey(references = Contact.class, on = "id", onDelete = Action.SET_NULL, onUpdate = Action.SET_NULL, since = @SQLVersion({ 1, 0, 0 }))
    private UUID acknowledgedById;

    /**
     * Has this alert recovered by itself
     */
    @SQLColumn(index = 22, name = "recovered", since = @SQLVersion({ 1, 0, 0 }))
    private boolean recovered = false;

    /**
     * Which check execution caused this alert to recover
     */
    @SQLColumn(index = 23, name = "recovered_by", since = @SQLVersion({ 1, 0, 0 }))
    private UUID recoveredBy;

    /**
     * When did this check recover
     */
    @SQLColumn(index = 24, name = "recovered_at", since = @SQLVersion({ 1, 0, 0 }))
    private Timestamp recoveredAt;

    /**
     * Was this alert escalated
     */
    @SQLColumn(index = 25, name = "escalated", since = @SQLVersion({ 3, 23, 0 }))
    private boolean escalated;

    /**
     * When was this alert first escalated
     */
    @SQLColumn(index = 26, name = "escalated_at", since = @SQLVersion({ 3, 23, 0 }))
    private Timestamp escalatedAt;

    /**
     * Who was notified because of this alert
     */
    @SQLColumn(index = 27, name = "notified_ids", type = "UUID[]", since = @SQLVersion({ 3, 26, 0 }))
    private List<UUID> notifiedIds = new LinkedList<UUID>();

    /**
     * The threshold for the next escalation
     */
    @SQLColumn(index = 28, name = "escalation_threshold", since = @SQLVersion({ 3, 34, 0 }))
    private long escalationThreshold = 0L;

    /**
     * Has this alert been marked as a false positive
     */
    @SQLColumn(index = 29, name = "false_positive", since = @SQLVersion({ 3, 52, 0 }))
    private boolean falsePositive = false;

    /**
     * When was this alert marked as a false positive
     */
    @SQLColumn(index = 30, name = "false_positive_at", since = @SQLVersion({ 3, 52, 0 }))
    private Timestamp falsePositiveAt;

    /**
     * Whom marked this alert as a false positive
     */
    @SQLColumn(index = 31, name = "false_positive_by_id", since = @SQLVersion({ 3, 52, 0 }))
    @SQLForeignKey(references = Contact.class, on = "id", onDelete = Action.SET_NULL, onUpdate = Action.SET_NULL, since = @SQLVersion({ 3, 52, 0 }))
    private UUID falsePositiveById;

    /**
     * Why this alert was marked as a false positive
     */
    @SQLColumn(index = 32, name = "false_positive_reason_id", since = @SQLVersion({ 3, 52, 0 }))
    private UUID falsePositiveReasonId;

    public Alert()
    {
        super();
    }

    public Alert(Check<?, ?> check, CheckState state, List<ContactMO> to)
    {
        this.siteId = check.getSiteId();
        this.id = state.getCurrentAlertId();
        this.checkId = check.getId();
        this.raised = new Timestamp(System.currentTimeMillis());
        // copy the state
        this.attempt = state.getAttempt();
        this.lastCheckId = state.getLastCheckId();
        this.lastCheckTime = state.getLastCheckTime();
        this.lastHardOutput = state.getLastHardOutput();
        this.lastHardStatus = state.getLastHardStatus();
        this.lastStateChange = state.getLastStateChange();
        this.okHistory = state.getOkHistory();
        this.output = state.getOutput();
        this.status = state.getStatus();
        this.flapping = state.isFlapping();
        this.hard = state.isHard();
        this.lastHardOk = state.isLastHardOk();
        this.ok = state.isOk();
        this.transitioning = state.isTransitioning();
        this.notifiedIds = to.stream().map(ContactMO::getId).collect(Collectors.toList());
        // defaults for other state
        this.acknowledged = false;
        this.acknowledgedAt = null;
        this.acknowledgedById = null;
        this.recovered = false;
        this.recoveredAt = null;
        this.recoveredBy = null;
        this.escalated = false;
        this.escalatedAt = null;
        this.falsePositive = false;
        this.falsePositiveAt = null;
        this.falsePositiveById = null;
        this.falsePositiveReasonId = null;
    }

    public UUID getSiteId()
    {
        return siteId;
    }

    public void setSiteId(UUID siteId)
    {
        this.siteId = siteId;
    }

    public UUID getId()
    {
        return id;
    }

    public void setId(UUID id)
    {
        this.id = id;
    }

    public UUID getCheckId()
    {
        return checkId;
    }

    public void setCheckId(UUID checkId)
    {
        this.checkId = checkId;
    }

    public Timestamp getRaised()
    {
        return raised;
    }

    public void setRaised(Timestamp raised)
    {
        this.raised = raised;
    }

    public boolean isOk()
    {
        return ok;
    }

    public void setOk(boolean ok)
    {
        this.ok = ok;
    }

    public Status getStatus()
    {
        return status;
    }

    public void setStatus(Status status)
    {
        this.status = status;
    }

    public String getOutput()
    {
        return output;
    }

    public void setOutput(String output)
    {
        this.output = output;
    }

    public Timestamp getLastCheckTime()
    {
        return lastCheckTime;
    }

    public void setLastCheckTime(Timestamp lastCheckTime)
    {
        this.lastCheckTime = lastCheckTime;
    }

    public UUID getLastCheckId()
    {
        return lastCheckId;
    }

    public void setLastCheckId(UUID lastCheckId)
    {
        this.lastCheckId = lastCheckId;
    }

    public int getAttempt()
    {
        return attempt;
    }

    public void setAttempt(int attempt)
    {
        this.attempt = attempt;
    }

    public boolean isHard()
    {
        return hard;
    }

    public void setHard(boolean hard)
    {
        this.hard = hard;
    }

    public boolean isTransitioning()
    {
        return transitioning;
    }

    public void setTransitioning(boolean transitioning)
    {
        this.transitioning = transitioning;
    }

    public boolean isFlapping()
    {
        return flapping;
    }

    public void setFlapping(boolean flapping)
    {
        this.flapping = flapping;
    }

    public Timestamp getLastStateChange()
    {
        return lastStateChange;
    }

    public void setLastStateChange(Timestamp lastStateChange)
    {
        this.lastStateChange = lastStateChange;
    }

    public long getOkHistory()
    {
        return okHistory;
    }

    public void setOkHistory(long okHistory)
    {
        this.okHistory = okHistory;
    }

    public boolean isLastHardOk()
    {
        return lastHardOk;
    }

    public void setLastHardOk(boolean lastHardOk)
    {
        this.lastHardOk = lastHardOk;
    }

    public Status getLastHardStatus()
    {
        return lastHardStatus;
    }

    public void setLastHardStatus(Status lastHardStatus)
    {
        this.lastHardStatus = lastHardStatus;
    }

    public String getLastHardOutput()
    {
        return lastHardOutput;
    }

    public void setLastHardOutput(String lastHardOutput)
    {
        this.lastHardOutput = lastHardOutput;
    }

    public boolean isAcknowledged()
    {
        return acknowledged;
    }

    public void setAcknowledged(boolean acknowledged)
    {
        this.acknowledged = acknowledged;
    }

    public Timestamp getAcknowledgedAt()
    {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(Timestamp acknowledgedAt)
    {
        this.acknowledgedAt = acknowledgedAt;
    }

    public UUID getAcknowledgedById()
    {
        return acknowledgedById;
    }

    public void setAcknowledgedById(UUID acknowledgedById)
    {
        this.acknowledgedById = acknowledgedById;
    }

    public boolean isRecovered()
    {
        return recovered;
    }

    public void setRecovered(boolean recovered)
    {
        this.recovered = recovered;
    }

    public UUID getRecoveredBy()
    {
        return recoveredBy;
    }

    public void setRecoveredBy(UUID recoveredBy)
    {
        this.recoveredBy = recoveredBy;
    }

    public Timestamp getRecoveredAt()
    {
        return recoveredAt;
    }

    public void setRecoveredAt(Timestamp recoveredAt)
    {
        this.recoveredAt = recoveredAt;
    }

    public boolean isEscalated()
    {
        return escalated;
    }

    public void setEscalated(boolean escalated)
    {
        this.escalated = escalated;
    }

    public Timestamp getEscalatedAt()
    {
        return escalatedAt;
    }

    public void setEscalatedAt(Timestamp escalatedAt)
    {
        this.escalatedAt = escalatedAt;
    }

    public List<UUID> getNotifiedIds()
    {
        return notifiedIds;
    }

    public void setNotifiedIds(List<UUID> notifiedIds)
    {
        this.notifiedIds = notifiedIds;
    }

    /**
     * Is this alert active, IE: not recovered and not acknowledged
     * 
     * @return
     */
    public boolean isActive()
    {
        return !(this.recovered || this.acknowledged);
    }

    /**
     * Get comments against this alert
     * 
     * @param limit
     *            the maximum number of comments to get
     */
    @Override
    public List<Comment> getComments(int limit)
    {
        try (BergamotDB db = BergamotDB.connect())
        {
            return db.getCommentsForObject(this.getId(), 0, limit);
        }
    }

    /**
     * Get comments against this alert
     */
    @Override
    public List<Comment> getComments()
    {
        return this.getComments(5);
    }

    /**
     * The check
     */
    public Check<?, ?> getCheck()
    {
        try (BergamotDB db = BergamotDB.connect())
        {
            return db.getCheck(this.getCheckId());
        }
    }

    public Contact getAcknowledgedBy()
    {
        if (this.getAcknowledgedById() == null) return null;
        try (BergamotDB db = BergamotDB.connect())
        {
            return db.getContact(this.getAcknowledgedById());
        }
    }

    public List<Contact> getNotified()
    {
        try (BergamotDB db = BergamotDB.connect())
        {
            return this.getNotifiedIds().stream().map((id) -> db.getContact(id)).filter((c) -> c != null).collect(Collectors.toList());
        }
    }

    public List<AlertEscalation> getEscalations()
    {
        try (BergamotDB db = BergamotDB.connect())
        {
            return db.getAlertEscalations(this.getId());
        }
    }

    public List<AlertEncompasses> getEncompasses()
    {
        try (BergamotDB db = BergamotDB.connect())
        {
            return db.getAlertEncompasses(this.getId());
        }
    }

    public long getEscalationThreshold()
    {
        return escalationThreshold;
    }

    public void setEscalationThreshold(long escalationThreshold)
    {
        this.escalationThreshold = escalationThreshold;
    }

    public boolean isFalsePositive()
    {
        return falsePositive;
    }

    public void setFalsePositive(boolean falsePositive)
    {
        this.falsePositive = falsePositive;
    }

    public Timestamp getFalsePositiveAt()
    {
        return falsePositiveAt;
    }

    public void setFalsePositiveAt(Timestamp falsePositiveAt)
    {
        this.falsePositiveAt = falsePositiveAt;
    }

    public UUID getFalsePositiveById()
    {
        return falsePositiveById;
    }

    public void setFalsePositiveById(UUID falsePositiveById)
    {
        this.falsePositiveById = falsePositiveById;
    }
    
    public Contact getFalsePositiveBy()
    {
        if (this.falsePositiveById == null) return null;
        try (BergamotDB db = BergamotDB.connect())
        {
            return db.getContact(this.falsePositiveById);
        }
    }

    public UUID getFalsePositiveReasonId()
    {
        return falsePositiveReasonId;
    }

    public void setFalsePositiveReasonId(UUID falsePositiveReasonId)
    {
        this.falsePositiveReasonId = falsePositiveReasonId;
    }
    
    public Comment getFalsePositiveReason()
    {
        if (this.falsePositiveReasonId == null) return null;
        try (BergamotDB db = BergamotDB.connect())
        {
            return db.getComment(this.falsePositiveReasonId);
        }
    }

    @Override
    public AlertMO toMO(Contact contact, EnumSet<MOFlag> options)
    {
        AlertMO mo = new AlertMO();
        mo.setAcknowledged(this.isAcknowledged());
        mo.setAcknowledgedAt(this.getAcknowledgedAt() == null ? -1 : this.getAcknowledgedAt().getTime());
        Contact ackedBy = this.getAcknowledgedBy();
        if (ackedBy != null && (contact == null || contact.hasPermission("read", ackedBy))) mo.setAcknowledgedBy(ackedBy.toStubMO(contact));
        mo.setCheck(this.getCheck().toMO(contact));
        mo.setFlapping(this.isFlapping());
        mo.setHard(this.isHard());
        mo.setId(this.getId());
        mo.setLastCheckId(this.getLastCheckId());
        mo.setLastCheckTime(this.getLastCheckTime() == null ? -1 : this.getLastCheckTime().getTime());
        mo.setLastHardOk(this.isLastHardOk());
        mo.setLastHardOutput(this.getLastHardOutput());
        mo.setLastHardStatus(this.getLastHardStatus().toString());
        mo.setLastStateChange(this.getLastStateChange() == null ? -1 : this.getLastStateChange().getTime());
        mo.setOk(this.isOk());
        mo.setOutput(this.getOutput());
        mo.setRaised(this.getRaised().getTime());
        mo.setRecovered(this.isRecovered());
        mo.setRecoveredAt(this.getRecoveredAt() == null ? -1 : this.getRecoveredAt().getTime());
        mo.setRecoveredBy(this.getRecoveredBy());
        mo.setStatus(this.getStatus().toString());
        mo.setTransitioning(this.isTransitioning());
        if (options.contains(MOFlag.COMMENTS))
        {
            mo.setComments(this.getComments().stream().map((x) -> x.toMO(contact)).collect(Collectors.toList()));
        }
        mo.setEscalated(this.isEscalated());
        mo.setEscalatedAt(this.getEscalatedAt() == null ? -1 : this.getEscalatedAt().getTime());
        mo.setNotified(this.getNotified().stream().filter((c) -> contact == null || contact.hasPermission("read", c)).map((c) -> c.toStubMO(contact)).collect(Collectors.toList()));
        mo.setEscalations(this.getEscalations().stream().map((e) -> e.toStubMO(contact)).collect(Collectors.toList()));
        mo.setEncompassed(this.getEncompasses().stream().map((e) -> e.toStubMO(contact)).collect(Collectors.toList()));
        // false positive
        mo.setFalsePositive(this.isFalsePositive());
        mo.setFalsePositiveAt(this.getFalsePositiveAt() == null ? -1 : this.getFalsePositiveAt().getTime());
        Contact fpBy = this.getFalsePositiveBy();
        if (fpBy != null && (contact == null || contact.hasPermission("read", fpBy))) mo.setFalsePositiveBy(fpBy.toStubMO(contact));
        Comment fpr = this.getFalsePositiveReason();
        if (fpr != null) mo.setFalsePositiveReason(fpr.toStubMO(contact));
        //
        return mo;
    }

    /**
     * Construct a generic notification based of this alert.
     * 
     * TODO: rebase the notification messages on this alert object
     * 
     * @param now
     *            when the alert is raise (ie: now)
     * @param type
     *            the notification type: alert, recovery, acknowledge
     * @param ctor
     *            the constructor for the notification type
     * @return the notification
     */
    public <T extends AlertNotification> T createNotification(Calendar now, NotificationType type, Supplier<T> ctor, List<ContactMO> to)
    {
        Check<?, ?> check = this.getCheck();
        CheckState state = check.getState();
        // check if to send
        if (!check.getNotifications().isEnabledAt(type, state.getStatus(), now)) return null;
        // create the notifications
        T notification = ctor.get();
        // the site
        notification.setSite(check.getSite().toMOUnsafe());
        notification.setRaised(now.getTimeInMillis());
        // alert id
        notification.setAlertId(this.getId());
        // send
        notification.setCheck(check.toMOUnsafe());
        // to
        notification.setTo(to);
        return notification;
    }

    public SendAlert createAlertNotification(Calendar now, List<ContactMO> to)
    {
        return this.createNotification(now, NotificationType.ALERT, SendAlert::new, to);
    }

    public SendAlert createEscalatedAlertNotification(Calendar now, List<ContactMO> to)
    {
        SendAlert sa = this.createNotification(now, NotificationType.ALERT, SendAlert::new, to);
        sa.setEscalation(true);
        return sa;
    }

    public SendRecovery createRecoveryNotification(Calendar now)
    {
        List<ContactMO> to = this.getContactsToNotify(this.getCheck(), this.getCheck().getState().getStatus(), now);
        return this.createNotification(now, NotificationType.RECOVERY, SendRecovery::new, to);
    }

    public SendAcknowledge createAcknowledgeNotification(Calendar now, Contact acknowledgedBy, Comment acknowledgeComment)
    {
        List<ContactMO> to = this.getContactsToNotify(this.getCheck(), this.getCheck().getState().getStatus(), now);
        SendAcknowledge sa = this.createNotification(now, NotificationType.ACKNOWLEDGE, SendAcknowledge::new, to);
        if (sa == null) return null;
        // additional detail for acknowledge
        sa.setAcknowledgedBy(acknowledgedBy.toStubMOUnsafe());
        sa.setAcknowledgeSummary(acknowledgeComment.getSummary());
        sa.setAcknowledgeComment(acknowledgeComment.getComment());
        // done
        return sa;
    }

    /**
     * Compute the list of contacts who should be notified on recovery or acknowledgement
     * 
     * @return a list of contact message objects
     */
    public List<ContactMO> getContactsToNotify(Check<?, ?> check, Status status, Calendar time)
    {
        final Notifications checkNotifications = check.getNotifications();
        // compute the engines available
        final Set<String> enabledEngines = checkNotifications.getEnginesEnabledAt(NotificationType.ALERT, status, time);
        // compute the contacts to notify
        return this.getNotified().stream().filter((contact) -> contact.getNotifications().isEnabledAt(NotificationType.ALERT, status, time)).map((contact) -> {
            ContactMO cmo = contact.toMOUnsafe();
            cmo.setEngines(contact.getNotifications().getEnginesEnabledAt(NotificationType.ALERT, status, time).stream().filter((engine) -> checkNotifications.isAllEnginesEnabled() || enabledEngines.contains(engine)).collect(Collectors.toSet()));
            return cmo;
        }).collect(Collectors.toList());
    }

    public List<ContactMO> getContactsToNotify(Calendar time)
    {
        return this.getContactsToNotify(this.getCheck(), this.getCheck().getState().getStatus(), time);
    }
}
