package com.goaltracker.exception;

/** Thrown for bad user input; the message is safe to show to the user. */
public class ValidationException extends AppException {
    public ValidationException(String message) { super(message); }
}
