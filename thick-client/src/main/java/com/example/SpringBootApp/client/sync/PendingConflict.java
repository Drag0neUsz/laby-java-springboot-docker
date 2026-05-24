package com.example.SpringBootApp.client.sync;

/**
 * Represents a conflict that the user explicitly chose NOT to resolve during
 * the previous synchronization attempt. The console keeps these around so the
 * user can review and resolve them later via menu option (f).
 */
public class PendingConflict {

    public enum Kind {
        DELETED_ON_SERVER,
        MODIFIED_ON_SERVER,
        BUSINESS_RULE_VIOLATED
    }

    private final Kind kind;
    private final ChangeRecord record;
    private final Object localPayload;
    private final Object serverPayload;
    private final String message;

    public PendingConflict(Kind kind, ChangeRecord record, Object localPayload, Object serverPayload, String message) {
        this.kind = kind;
        this.record = record;
        this.localPayload = localPayload;
        this.serverPayload = serverPayload;
        this.message = message;
    }

    public Kind getKind() { return kind; }
    public ChangeRecord getRecord() { return record; }
    public Object getLocalPayload() { return localPayload; }
    public Object getServerPayload() { return serverPayload; }
    public String getMessage() { return message; }

    @Override
    public String toString() {
        return "[" + kind + "] " + record + " :: " + message;
    }
}
