package com.goaltracker.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Goal extends BaseEntity {
    private int userId, goalTypeId;
    private String name, typeName = "Uncategorised", status = "ACTIVE";
    private double targetHours;
    private LocalDate deadline;
    private long loggedMinutes;

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getGoalTypeId() { return goalTypeId; }
    public void setGoalTypeId(int goalTypeId) { this.goalTypeId = goalTypeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { if (typeName != null) this.typeName = typeName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getTargetHours() { return targetHours; }
    public void setTargetHours(double targetHours) { this.targetHours = targetHours; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
    public long getLoggedMinutes() { return loggedMinutes; }
    public void setLoggedMinutes(long loggedMinutes) { this.loggedMinutes = loggedMinutes; }

    public double getLoggedHours() { return loggedMinutes / 60.0; }
    public boolean isCompleted() { return "COMPLETED".equals(status); }
    public boolean isOverdue() { return !isCompleted() && deadline != null && deadline.isBefore(LocalDate.now()); }
    public long getDaysLeft() { return ChronoUnit.DAYS.between(LocalDate.now(), deadline); }
    public int getProgressPercent() {
        double targetMinutes = targetHours * 60;
        return targetMinutes <= 0 ? 0 : (int) Math.min(100, Math.round(loggedMinutes * 100.0 / targetMinutes));
    }
    @Override public String describe() { return name + " - " + getProgressPercent() + "% (due " + deadline + ")"; }
}
