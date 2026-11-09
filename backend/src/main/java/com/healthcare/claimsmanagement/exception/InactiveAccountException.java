package com.healthcare.claimsmanagement.exception;

public class InactiveAccountException extends RuntimeException {
    public InactiveAccountException(String message) { super(message); }
}
