package com.example.workonit.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Goal implements Serializable {

    public enum Type { POSITIVE, NEGATIVE }
    // lifecycle status for color-coding & actions
    public enum Status { ACTIVE, ON_HOLD, COMPLETED, EXPIRED }
    public final String id;
    public final String name;
    public final Type type;
    public final int timesPerWeek;   // 0 means not specified yet
    public final long createdAtUtc;  // epoch millis
    public Long dueAtUtc = null;
    public boolean autoExpire = true;
    // current status (default ACTIVE)
    public Status status = Status.ACTIVE;
    public String notes = ""; // freeform notes
    // map of date string (e.g. "2025-08-17") to a progress entry
    public final Map<String, ProgressEntry> progress = new HashMap<>();

    public Goal(String name, Type type, int timesPerWeek, long createdAtUtc) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.type = type;
        this.timesPerWeek = timesPerWeek;
        this.createdAtUtc = createdAtUtc;
    }
    // ---- convenience setters we’ll call from the UI actions
    public void markActive()    { this.status = Status.ACTIVE; }
    public void markOnHold()    { this.status = Status.ON_HOLD; }
    public void markCompleted() { this.status = Status.COMPLETED; }
    public void markExpired()   { this.status = Status.EXPIRED; }
    // ---- helpers for expiration ----
    /** Returns true if this goal has a due date and it's in the past (nowUtc > dueAtUtc). */
    public boolean isPastDue(long nowUtc) {
        return autoExpire && dueAtUtc != null && dueAtUtc > 0 && nowUtc > dueAtUtc;
    }
    /** If past due, flip status to EXPIRED (idempotent). Returns true if changed. */
    public boolean expireIfNeeded(long nowUtc) {
        if (isPastDue(nowUtc) && status != Status.EXPIRED && status != Status.COMPLETED) {
            status = Status.EXPIRED;
            return true;
        }
        return false;
    }
    // nested helper class for ratings
    public static class ProgressEntry implements Serializable {
        public int effort;
        public int temptation;
        public int mood;
        public String comment;

        public ProgressEntry(int effort, int temptation, int mood, String comment) {
            this.effort = effort;
            this.temptation = temptation;
            this.mood = mood;
            this.comment = comment;
        }
    }
}