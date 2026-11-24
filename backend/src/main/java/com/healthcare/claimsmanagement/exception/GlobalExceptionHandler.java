
package com.healthcare.claimsmanagement.exception;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.server.ResponseStatusException;

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

    @ExceptionHandler(DuplicateResourceException.class)
    ResponseEntity<ApiError> duplicateResource(
            DuplicateResourceException ex,
            HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> notFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<ApiError> badRequest(
            BadRequestException ex,
            HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
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

    // HANDLE RESPONSE STATUS EXCEPTIONS
    // Preserves intended HTTP status: 400, 401, 403, 404, 409, etc.
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> statusException(
            ResponseStatusException ex,
            HttpServletRequest request) {

        HttpStatusCode status = ex.getStatusCode();

        String message = ex.getReason() == null
                ? "Request failed"
                : ex.getReason();

        log.warn(
                "Request rejected: {} {} - HTTP {}",
                request.getMethod(),
                request.getRequestURI(),
                status.value()
        );

        return response(
                status,
                message,
                request,
                null
        );
    }

    // 500 - UNEXPECTED SERVER ERRORS
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(
            Exception ex,
            HttpServletRequest request) {

        log.error(
                "Unexpected error while processing {} {}",
                request.getMethod(),
                request.getRequestURI(),
                ex
        );

        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                request,
                null
        );
    }

    // STANDARD API ERROR RESPONSE
    private ResponseEntity<ApiError> response(
            HttpStatusCode status,
            String message,
            HttpServletRequest request,
            Map<String, String> fields) {

        HttpStatus resolvedStatus =
                HttpStatus.resolve(status.value());

        String error = resolvedStatus == null
                ? "HTTP Error"
                : resolvedStatus.getReasonPhrase();

        ApiError apiError = new ApiError(
                Instant.now(),
                status.value(),
                error,
                message,
                request.getRequestURI(),
                fields
        );

        return ResponseEntity
                .status(status)
                .body(apiError);
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
