package com.goaltracker.exception;

/** Checked exception for any application-level failure (wraps SQL errors). */
public class AppException extends Exception {
    public AppException(String message) { super(message); }
    public AppException(String message, Throwable cause) { super(message, cause); }
}
