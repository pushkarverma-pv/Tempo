package com.goaltracker.util;

import static org.junit.jupiter.api.Assertions.*;

import com.goaltracker.exception.ValidationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ValidatorTest {

    @Test
    void requireTextTrimsAndReturnsValue() throws ValidationException {
        assertEquals("Study Java", Validator.requireText("  Study Java  ", "Goal name", 100));
    }

    @Test
    void requireTextRejectsBlankAndTooLong() {
        assertThrows(ValidationException.class, () -> Validator.requireText("   ", "Goal name", 100));
        assertThrows(ValidationException.class, () -> Validator.requireText(null, "Goal name", 100));
        assertThrows(ValidationException.class, () -> Validator.requireText("abcdef", "Goal name", 5));
    }

    @Test
    void requireEmailLowercasesValidAddress() throws ValidationException {
        assertEquals("a.b@tempo.app", Validator.requireEmail("  A.B@Tempo.App "));
    }

    @Test
    void requireEmailRejectsInvalidAddress() {
        assertThrows(ValidationException.class, () -> Validator.requireEmail("not-an-email"));
        assertThrows(ValidationException.class, () -> Validator.requireEmail("a@b"));
    }

    @Test
    void requirePasswordNeedsSixCharacters() throws ValidationException {
        assertEquals("secret1", Validator.requirePassword("secret1"));
        assertThrows(ValidationException.class, () -> Validator.requirePassword("12345"));
        assertThrows(ValidationException.class, () -> Validator.requirePassword(null));
    }

    @Test
    void requireIdParsesNumbersOnly() throws ValidationException {
        assertEquals(42, Validator.requireId(" 42 ", "goal"));
        assertThrows(ValidationException.class, () -> Validator.requireId("abc", "goal"));
        assertThrows(ValidationException.class, () -> Validator.requireId(null, "goal"));
    }

    @Test
    void requirePositiveEnforcesRange() throws ValidationException {
        assertEquals(2.5, Validator.requirePositive("2.5", "Target hours", 10));
        assertThrows(ValidationException.class, () -> Validator.requirePositive("0", "Target hours", 10));
        assertThrows(ValidationException.class, () -> Validator.requirePositive("-1", "Target hours", 10));
        assertThrows(ValidationException.class, () -> Validator.requirePositive("11", "Target hours", 10));
        assertThrows(ValidationException.class, () -> Validator.requirePositive("abc", "Target hours", 10));
    }

    @Test
    void requireFutureDateAcceptsTodayAndLater() throws ValidationException {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        assertEquals(tomorrow, Validator.requireFutureDate(tomorrow.toString()));
        assertEquals(LocalDate.now(), Validator.requireFutureDate(LocalDate.now().toString()));
    }

    @Test
    void requireFutureDateRejectsPastAndGarbage() {
        assertThrows(ValidationException.class, () -> Validator.requireFutureDate(LocalDate.now().minusDays(1).toString()));
        assertThrows(ValidationException.class, () -> Validator.requireFutureDate("31-12-2030"));
        assertThrows(ValidationException.class, () -> Validator.requireFutureDate(null));
    }

    @Test
    void requireDateTimeParsesBrowserFormat() throws ValidationException {
        assertEquals(LocalDateTime.of(2026, 10, 15, 9, 30), Validator.requireDateTime("2026-10-15T09:30", "start time"));
        assertThrows(ValidationException.class, () -> Validator.requireDateTime("yesterday", "start time"));
    }
}
