package com.example.SpringBootApp.client.sync;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * One entry in the offline action queue (changelog).
 *
 * - {@link #entityId} is the id KNOWN TO THE CLIENT at the moment the action
 *   was recorded. For freshly-created entities this is a temporary negative id;
 *   after that record is synchronized successfully the queue rewrites the id
 *   to the server-assigned positive id (so subsequent UPDATE/DELETE records
 *   referencing the same temp id can be merged correctly).
 * - {@link #payload} is the full DTO snapshot at the time of the action
 *   (null for DELETE).
 */
public class ChangeRecord {

    private static final AtomicLong SEQ = new AtomicLong(0);

    private final long sequence;
    private final EntityType entityType;
    private ActionType actionType;
    private Integer entityId;
    private Object payload;
    private final LocalDateTime createdAt;

    public ChangeRecord(EntityType entityType, ActionType actionType, Integer entityId, Object payload) {
        this.sequence = SEQ.incrementAndGet();
        this.entityType = entityType;
        this.actionType = actionType;
        this.entityId = entityId;
        this.payload = payload;
        this.createdAt = LocalDateTime.now();
    }

    public long getSequence() { return sequence; }
    public EntityType getEntityType() { return entityType; }
    public ActionType getActionType() { return actionType; }
    public void setActionType(ActionType actionType) { this.actionType = actionType; }
    public Integer getEntityId() { return entityId; }
    public void setEntityId(Integer entityId) { this.entityId = entityId; }
    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return "#" + sequence + " " + actionType + " " + entityType + "(id=" + entityId + ")"
                + (payload != null ? " payload=" + payload : "");
    }
}
