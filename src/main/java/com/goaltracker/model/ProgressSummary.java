package com.goaltracker.model;

import java.util.Map;

/** Read-only summary of a user's goals, built by ProgressService. */
public class ProgressSummary {
    private final int total, completed, active, overdue, averagePercent;
    private final double totalHours;
    private final Map<String, Long> byType;

    public ProgressSummary(int total, int completed, int overdue, int averagePercent, double totalHours, Map<String, Long> byType) {
        this.total = total; this.completed = completed; this.active = total - completed;
        this.overdue = overdue; this.averagePercent = averagePercent; this.totalHours = totalHours; this.byType = byType;
    }
    public int getTotal() { return total; }
    public int getCompleted() { return completed; }
    public int getActive() { return active; }
    public int getOverdue() { return overdue; }
    public int getAveragePercent() { return averagePercent; }
    public double getTotalHours() { return totalHours; }
    public Map<String, Long> getByType() { return byType; }
}
