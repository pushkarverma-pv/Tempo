package com.goaltracker.service;

import static org.junit.jupiter.api.Assertions.*;

import com.goaltracker.model.Goal;
import com.goaltracker.model.ProgressSummary;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProgressServiceTest {

    private final ProgressService service = new ProgressService();

    private Goal goal(String type, double targetHours, long loggedMinutes, int deadlineOffsetDays, String status) {
        Goal g = new Goal();
        g.setTypeName(type);
        g.setTargetHours(targetHours);
        g.setLoggedMinutes(loggedMinutes);
        g.setDeadline(LocalDate.now().plusDays(deadlineOffsetDays));
        g.setStatus(status);
        return g;
    }

    @Test
    void emptyListGivesZeroSummary() {
        ProgressSummary s = service.summarize(List.of());
        assertEquals(0, s.getTotal());
        assertEquals(0, s.getAveragePercent());
        assertEquals(0.0, s.getTotalHours());
        assertTrue(s.getByType().isEmpty());
    }

    @Test
    void countsCompletedActiveAndOverdue() {
        ProgressSummary s = service.summarize(List.of(
            goal("Study", 10, 600, 5, "COMPLETED"),
            goal("Study", 10, 60, 5, "ACTIVE"),
            goal("Fitness", 10, 0, -2, "ACTIVE")));
        assertEquals(3, s.getTotal());
        assertEquals(1, s.getCompleted());
        assertEquals(2, s.getActive());
        assertEquals(1, s.getOverdue());
    }

    @Test
    void averagesProgressAndSumsHours() {
        ProgressSummary s = service.summarize(List.of(
            goal("Study", 10, 600, 5, "COMPLETED"),   // 100%
            goal("Study", 10, 0, 5, "ACTIVE")));      // 0%
        assertEquals(50, s.getAveragePercent());
        assertEquals(10.0, s.getTotalHours(), 0.0001);
    }

    @Test
    void groupsGoalsByTypeInAlphabeticalOrder() {
        ProgressSummary s = service.summarize(List.of(
            goal("Study", 1, 0, 5, "ACTIVE"),
            goal("Fitness", 1, 0, 5, "ACTIVE"),
            goal("Study", 1, 0, 5, "ACTIVE")));
        assertEquals(List.of("Fitness", "Study"), List.copyOf(s.getByType().keySet()));
        assertEquals(2L, s.getByType().get("Study"));
        assertEquals(1L, s.getByType().get("Fitness"));
    }
}
