package com.goaltracker.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class GoalTest {

    private Goal goal(double targetHours, long loggedMinutes, int deadlineOffsetDays, String status) {
        Goal g = new Goal();
        g.setName("Test goal");
        g.setTargetHours(targetHours);
        g.setLoggedMinutes(loggedMinutes);
        g.setDeadline(LocalDate.now().plusDays(deadlineOffsetDays));
        g.setStatus(status);
        return g;
    }

    @Test
    void progressPercentIsLoggedOverTarget() {
        assertEquals(50, goal(10, 300, 5, "ACTIVE").getProgressPercent());
    }

    @Test
    void progressPercentIsCappedAtHundred() {
        assertEquals(100, goal(1, 600, 5, "ACTIVE").getProgressPercent());
    }

    @Test
    void zeroTargetDoesNotDivideByZero() {
        assertEquals(0, goal(0, 100, 5, "ACTIVE").getProgressPercent());
    }

    @Test
    void activeGoalPastDeadlineIsOverdue() {
        assertTrue(goal(10, 0, -1, "ACTIVE").isOverdue());
    }

    @Test
    void completedGoalIsNeverOverdue() {
        assertFalse(goal(10, 600, -1, "COMPLETED").isOverdue());
    }

    @Test
    void daysLeftCountsFromToday() {
        assertEquals(3, goal(10, 0, 3, "ACTIVE").getDaysLeft());
    }

    @Test
    void describeMentionsNameAndProgress() {
        assertTrue(goal(10, 300, 5, "ACTIVE").describe().contains("50%"));
    }
}
