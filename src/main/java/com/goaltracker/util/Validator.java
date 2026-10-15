package com.goaltracker.util;

import com.goaltracker.exception.ValidationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/** Server-side input validation; every method throws ValidationException with a user-friendly message. */
public final class Validator {
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private Validator() { }

    public static String requireText(String v, String field, int max) throws ValidationException {
        if (v == null || v.isBlank()) throw new ValidationException(field + " is required");
        v = v.trim();
        if (v.length() > max) throw new ValidationException(field + " must be at most " + max + " characters");
        return v;
    }

    public static String requireEmail(String v) throws ValidationException {
        v = requireText(v, "Email", 150).toLowerCase();
        if (!EMAIL.matcher(v).matches()) throw new ValidationException("Enter a valid email address");
        return v;
    }

    public static String requirePassword(String v) throws ValidationException {
        if (v == null || v.length() < 6) throw new ValidationException("Password must be at least 6 characters");
        return v;
    }

    public static int requireId(String v, String field) throws ValidationException {
        try { return Integer.parseInt(v.trim()); }
        catch (RuntimeException e) { throw new ValidationException("Invalid " + field); }
    }

    public static double requirePositive(String v, String field, double max) throws ValidationException {
        double d;
        try { d = Double.parseDouble(v.trim()); }
        catch (RuntimeException e) { throw new ValidationException(field + " must be a number"); }
        if (d <= 0 || d > max) throw new ValidationException(field + " must be between 0 and " + (int) max);
        return d;
    }

    public static LocalDate requireFutureDate(String v) throws ValidationException {
        LocalDate d;
        try { d = LocalDate.parse(v.trim()); }
        catch (DateTimeParseException | NullPointerException e) { throw new ValidationException("Enter a valid deadline date"); }
        if (d.isBefore(LocalDate.now())) throw new ValidationException("Deadline cannot be in the past");
        return d;
    }

    public static LocalDateTime requireDateTime(String v, String field) throws ValidationException {
        try { return LocalDateTime.parse(v.trim()); }
        catch (DateTimeParseException | NullPointerException e) { throw new ValidationException("Enter a valid " + field); }
    }
}
