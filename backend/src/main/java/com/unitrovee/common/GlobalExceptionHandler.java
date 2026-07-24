package com.unitrovee.common;

import com.unitrovee.auth.exception.EmailAlreadyExistsException;
import com.unitrovee.auth.exception.UnsupportedSchoolEmailException;
import com.unitrovee.common.exception.ResourceNotFoundException;
import com.unitrovee.auth.exception.InvalidVerificationCodeException;
import com.unitrovee.item.exception.ItemNotEditableException;
import com.unitrovee.item.exception.InvalidItemUpdateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 - a domain "not found" exception bubbled up from a service
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        ErrorResponse body = ErrorResponse.of("RESOURCE_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }


    // 409 - an account already uses this normalized email address
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        ErrorResponse body = ErrorResponse.of("EMAIL_ALREADY_EXISTS", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }


    // 400 - valid email syntax, but not from an active supported school
    @ExceptionHandler(UnsupportedSchoolEmailException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedSchoolEmail(UnsupportedSchoolEmailException ex) {
        ErrorResponse body = ErrorResponse.of("UNSUPPORTED_SCHOOL_EMAIL", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }


    // 400 - Bean Validation failed on a @Valid request body
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        // build a readable message from the first field error.
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse("Validation failed");
        ErrorResponse body = ErrorResponse.of("VALIDATION_ERROR", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }


    // 400 - unknown, wrong, or expired email-verification code
    @ExceptionHandler(InvalidVerificationCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidVerificationCode(InvalidVerificationCodeException ex) {
        ErrorResponse body = ErrorResponse.of("INVALID_VERIFICATION_CODE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }


    // 400 - malformed JSON or unsupported enum value in a request body
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableRequest(HttpMessageNotReadableException ex) {
        ErrorResponse body = ErrorResponse.of(
                "VALIDATION_ERROR",
                "Request body contains an invalid value"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }


    // 401 - map invalid credentials to 401
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        ErrorResponse body = ErrorResponse.of(
                "INVALID_CREDENTIALS",
                "Invalid email or password"
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }


    @ExceptionHandler(ItemNotEditableException.class)
    public ResponseEntity<ErrorResponse> handleItemNotEditable(ItemNotEditableException ex) {
        ErrorResponse body = ErrorResponse.of("ITEM_NOT_EDITABLE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }


    @ExceptionHandler(InvalidItemUpdateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidItemUpdate(InvalidItemUpdateException ex) {
        ErrorResponse body = ErrorResponse.of("VALIDATION_ERROR", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }


    // 403 - authenticated user does not have the required role or permission
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponse body = ErrorResponse.of("FORBIDDEN", "Access denied");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }


    // 500 - catch-all safety net for anything we didn't explicitly handle.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        // Log the FULL detail server-side, but never leak internals to the client
        log.error("Unhandled exception", ex);
        ErrorResponse body = ErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}