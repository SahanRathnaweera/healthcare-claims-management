
package com.healthcare.claimsmanagement.exception;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);
 
    // 400 - VALIDATION ERRORS
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> fields = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage(),
                        (first, ignored) -> first
                ));

        return response(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request,
                fields
        );
    }
 
    // 401 - AUTHENTICATION ERRORS
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> unauthorized(
            AuthenticationException ex,
            HttpServletRequest request) {

        return response(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                request,
                null
        );
    }
 
    // 403 - INACTIVE ACCOUNT
    @ExceptionHandler(InactiveAccountException.class)
    ResponseEntity<ApiError> inactive(
            InactiveAccountException ex,
            HttpServletRequest request) {

        return response(
                HttpStatus.FORBIDDEN,
                ex.getMessage(),
                request,
                null
        );
    }
 
    // 409 - DUPLICATE EMAIL
    @ExceptionHandler(DuplicateEmailException.class)
    ResponseEntity<ApiError> conflict(
            DuplicateEmailException ex,
            HttpServletRequest request) {

        return response(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request,
                null
        );
    }
 
    // 400 - PASSWORD MISMATCH
    @ExceptionHandler(PasswordMismatchException.class)
    ResponseEntity<ApiError> mismatch(
            PasswordMismatchException ex,
            HttpServletRequest request) {

        return response(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request,
                null
        );
    }
 
    // 500 - UNEXPECTED SERVER ERRORS
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(
            Exception ex,
            HttpServletRequest request) {

        // Log the full exception for backend debugging.
        log.error(
                "Unexpected error while processing {} {}",
                request.getMethod(),
                request.getRequestURI(),
                ex
        );

        // Do not expose internal exception details to API users.
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                request,
                null
        );
    }
 
    // STANDARD API ERROR RESPONSE
     

    private ResponseEntity<ApiError> response(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, String> fields) {

        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                fields
        );

        return ResponseEntity
                .status(status)
                .body(error);
    }
 
    // API ERROR RESPONSE DTO    
    public record ApiError(
            Instant timestamp,
            int status,
            String error,
            String message,
            String path,
            Map<String, String> fieldErrors
    ) {
    }
}
