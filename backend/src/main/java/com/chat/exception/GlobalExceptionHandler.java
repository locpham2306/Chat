package com.chat.exception;

import com.chat.common.constant.ErrorConstants;
import com.chat.common.constant.StatusCode;
import com.chat.common.dto.response.ErrorDetailResponse;
import com.chat.common.dto.response.FieldErrorResponse;
import com.chat.common.wrapper.BaseResponse;
import com.chat.utils.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final MessageUtils messages;

    public GlobalExceptionHandler(MessageUtils messages) {
        this.messages = messages;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Object> handleBusinessException(BusinessException exception, WebRequest request) {
        var status = exception.getStatus();
        String strCode = ErrorConstants.forStatus(status.value());
        String detail = messages.getMessage(exception.getMessageKey(), exception.getArguments());
        if (detail.equals(exception.getMessageKey())) {
            detail = messages.getMessage(strCode);
        }
        return response(status, HttpHeaders.EMPTY, strCode, detail, List.of(), request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
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
                                    ? messages.getMessage(ErrorConstants.VALIDATION_ERROR) : error.getDefaultMessage()))
                    .toList();
        }
        var status = HttpStatus.valueOf(statusCode.value());
        String strCode = ErrorConstants.forStatus(status.value());
        return response(status, headers, strCode, messages.getMessage(strCode), fields, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpectedException(Exception exception, WebRequest request) {
        LOG.error("Unhandled exception type: {}", exception.getClass().getName());
        return response(HttpStatus.INTERNAL_SERVER_ERROR, HttpHeaders.EMPTY,
                ErrorConstants.INTERNAL_ERROR, messages.getMessage(ErrorConstants.INTERNAL_ERROR),
                List.of(), request);
    }

    private ResponseEntity<Object> response(
            HttpStatus status, HttpHeaders headers, String strCode, String message,
            List<FieldErrorResponse> fields, WebRequest request) {
        String path = request instanceof ServletWebRequest servletRequest
                ? servletRequest.getRequest().getRequestURI() : "";
        var detail = new ErrorDetailResponse(Instant.now(), status.value(), path, fields);
        var response = BaseResponse.error(StatusCode.from(status), strCode, message, detail);
        return new ResponseEntity<>(response, headers, status);
    }
}
