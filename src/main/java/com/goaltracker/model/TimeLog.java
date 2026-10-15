package com.goaltracker.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimeLog extends BaseEntity {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private int goalId, userId, durationMinutes;
    private String goalName;
    private LocalDateTime start, end;

    public int getGoalId() { return goalId; }
    public void setGoalId(int goalId) { this.goalId = goalId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public String getGoalName() { return goalName; }
    public void setGoalName(String goalName) { this.goalName = goalName; }
    public LocalDateTime getStart() { return start; }
    public void setStart(LocalDateTime start) { this.start = start; }
    public LocalDateTime getEnd() { return end; }
    public void setEnd(LocalDateTime end) { this.end = end; }

    public String getStartDisplay() { return start.format(FMT); }
    public String getEndDisplay() { return end.format(FMT); }
    public String getDurationDisplay() { return (durationMinutes / 60) + "h " + (durationMinutes % 60) + "m"; }
    @Override public String describe() { return goalName + ": " + getDurationDisplay(); }
}
