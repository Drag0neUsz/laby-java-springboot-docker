package com.example.SpringBootApp.client.sync;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Append-only changelog of every local modification performed while offline.
 * The console UI displays it on demand (option d) and the SyncService consumes
 * it during synchronization (option e).
 */
@Component
public class ActionQueue {

    private final List<ChangeRecord> records = new ArrayList<>();

    public synchronized void record(ChangeRecord r) {
        records.add(r);
    }

    public synchronized List<ChangeRecord> snapshot() {
        return new ArrayList<>(records);
    }

    public synchronized int size() {
        return records.size();
    }

    public synchronized boolean isEmpty() {
        return records.isEmpty();
    }

    public synchronized void clear() {
        records.clear();
    }

    /**
     * Replace the whole queue with a new (typically optimized / partially-applied)
     * version. Used by the SyncService after optimization and after partial success.
     */
    public synchronized void replaceWith(List<ChangeRecord> next) {
        records.clear();
        records.addAll(next);
    }
}
