package com.healthcare.claimsmanagement.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String message) { super(message); }
}
