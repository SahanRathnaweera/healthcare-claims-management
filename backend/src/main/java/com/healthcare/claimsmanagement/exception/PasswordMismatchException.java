package com.healthcare.claimsmanagement.exception;

public class PasswordMismatchException extends RuntimeException {
    public PasswordMismatchException(String message) { super(message); }
}
