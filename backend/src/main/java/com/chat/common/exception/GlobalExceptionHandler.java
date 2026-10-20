package com.chat.common.exception;

import com.chat.common.constant.ErrorConstants;
import com.chat.common.dto.response.ErrorResponse;
import com.chat.common.dto.response.FieldErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Object> handleBusinessException(BusinessException exception, WebRequest request) {
        String code = ErrorConstants.forStatus(exception.getStatus().value());
        String fallback = message(code, request);
        String detail = messageSource.getMessage(
                exception.getMessageKey(), exception.getArguments(), fallback, LocaleContextHolder.getLocale());
        return response(exception.getStatus(), HttpHeaders.EMPTY, code, detail, List.of(), request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest
                && servletRequest.getResponse() != null
                && servletRequest.getResponse().isCommitted()) {
            return null;
        }

        List<FieldErrorResponse> fields = List.of();
        if (exception instanceof MethodArgumentNotValidException validationException) {
            fields = validationException.getBindingResult().getAllErrors().stream()
                    .map(error -> new FieldErrorResponse(
                            error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName(),
                            error.getDefaultMessage() == null
                                    ? message(ErrorConstants.VALIDATION_ERROR, request) : error.getDefaultMessage()))
                    .toList();
        }
        String code = ErrorConstants.forStatus(status.value());
        return response(status, headers, code, message(code, request), fields, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpectedException(Exception exception, WebRequest request) {
        // Exception messages and causes may contain credentials or SQL.
        LOG.error("Unhandled exception type: {}", exception.getClass().getName());
        return response(HttpStatus.INTERNAL_SERVER_ERROR, HttpHeaders.EMPTY,
                ErrorConstants.INTERNAL_ERROR, message(ErrorConstants.INTERNAL_ERROR, request),
                List.of(), request);
    }

    private String message(String code, WebRequest request) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    private ResponseEntity<Object> response(
            HttpStatusCode status, HttpHeaders headers, String code, String message,
            List<FieldErrorResponse> fields, WebRequest request) {
        String path = request instanceof ServletWebRequest servletRequest
                ? servletRequest.getRequest().getRequestURI() : "";
        var error = new ErrorResponse(Instant.now(), status.value(), code, message, path, fields);
        return new ResponseEntity<>(error, headers, status);
    }
}
