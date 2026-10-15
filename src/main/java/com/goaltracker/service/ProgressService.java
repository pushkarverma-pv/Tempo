package com.goaltracker.service;

import com.goaltracker.model.Goal;
import com.goaltracker.model.ProgressSummary;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Builds the progress summary shown on the user dashboard (Collections + Streams). */
public class ProgressService {

    public ProgressSummary summarize(List<Goal> goals) {
        int completed = (int) goals.stream().filter(Goal::isCompleted).count();
        int overdue = (int) goals.stream().filter(Goal::isOverdue).count();
        int average = (int) Math.round(goals.stream().mapToInt(Goal::getProgressPercent).average().orElse(0));
        double hours = goals.stream().mapToLong(Goal::getLoggedMinutes).sum() / 60.0;
        var byType = goals.stream().collect(Collectors.groupingBy(Goal::getTypeName, TreeMap::new, Collectors.counting()));
        return new ProgressSummary(goals.size(), completed, overdue, average, hours, byType);
    }
}
