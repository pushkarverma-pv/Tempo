package com.goaltracker.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PasswordUtilTest {

    @Test
    void correctPasswordVerifies() {
        String stored = PasswordUtil.hash("Admin@123");
        assertTrue(PasswordUtil.verify("Admin@123", stored));
    }

    @Test
    void wrongPasswordFails() {
        String stored = PasswordUtil.hash("Admin@123");
        assertFalse(PasswordUtil.verify("admin@123", stored));
    }

    @Test
    void samePasswordGivesDifferentHashesBecauseOfSalt() {
        assertNotEquals(PasswordUtil.hash("same"), PasswordUtil.hash("same"));
    }

    @Test
    void storedValueIsNotThePlainPassword() {
        assertFalse(PasswordUtil.hash("hunter22").contains("hunter22"));
    }

    @Test
    void nullOrMalformedStoredValueNeverVerifies() {
        assertFalse(PasswordUtil.verify("x", null));
        assertFalse(PasswordUtil.verify("x", "no-colon-here"));
        assertFalse(PasswordUtil.verify(null, PasswordUtil.hash("x")));
    }
}
