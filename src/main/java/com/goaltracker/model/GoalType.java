package com.goaltracker.model;

public class GoalType extends BaseEntity {
    private String name, metric;
    private int maxTargetHours = 500;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }
    public int getMaxTargetHours() { return maxTargetHours; }
    public void setMaxTargetHours(int maxTargetHours) { this.maxTargetHours = maxTargetHours; }
    @Override public String describe() { return name + " (" + metric + ", max " + maxTargetHours + "h)"; }
}
