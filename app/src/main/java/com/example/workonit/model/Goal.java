package com.example.workonit.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.*;
import java.util.UUID;

// simple model for now; we’ll extend later with notes + per-day progress
public class Goal implements Serializable {

    public enum Type { POSITIVE, NEGATIVE }

    public final String id;
    public final String name;
    public final Type type;
    public final int timesPerWeek;   // 0 means not specified yet
    public final long createdAtUtc;  // epoch millis

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