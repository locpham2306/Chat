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
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// biến lỗi đó thành response gửi cho FE.
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final MessageUtils messageUtils;

    public GlobalExceptionHandler(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Object> handleBusinessException(BusinessException exception, WebRequest request) {
        HttpStatus status = exception.getStatus();
        String strCode = ErrorConstants.forStatus(status.value()); // Đổi status thành mã lỗi cho FE
        String detail = messageUtils.getMessage(exception.getMessageKey(), exception.getArguments()); // Dịch message
        if (detail.equals(exception.getMessageKey())) {
            detail = messageUtils.getMessage(strCode);
        }
        return response(status, HttpHeaders.EMPTY, strCode, detail, List.of(), request);
    }

    // Hàm này xử lý các lỗi do Spring MVC phát hiện trong quá trình đọc và kiểm tra request.
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        // Response đã gửi xuống FE thì không thể ghi thêm một response lỗi khác.
        if (request instanceof ServletWebRequest servletRequest
                && servletRequest.getResponse() != null
                && servletRequest.getResponse().isCommitted()) {
            return null;
        }
        // Mặc định không có lỗi theo field
        List<FieldErrorResponse> fields = List.of();
        // Kiểm tra có phải lỗi @Valid không
        if (exception instanceof MethodArgumentNotValidException validationException) {
            fields = new ArrayList<>();
            // Lấy toàn bộ lỗi validation
            for (ObjectError error : validationException.getBindingResult().getAllErrors()) {
                // Xác định field bị lỗi
                String field = error.getObjectName();
                if (error instanceof FieldError fieldError) {
                    field = fieldError.getField();
                }
                // Lấy message validation
                String message = error.getDefaultMessage();
                if (message == null) {
                    // dùng message validation chung làm phương án dự phòng.
                    message = messageUtils.getMessage(ErrorConstants.VALIDATION_ERROR);
                }
                fields.add(new FieldErrorResponse(field, message));
            }
        }
        // Giữ nguyên HTTP status của Spring
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        // Đổi status thành strCode
        String strCode = ErrorConstants.forStatus(status.value());
        String message = messageUtils.getMessage(strCode);
        return response(status, headers, strCode, message, fields, request);
    }

    // Lỗi bất ngờ mà dự án không dự kiến, bắt mọi Exception chưa có handler cụ thể hơn.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpectedException(Exception exception, WebRequest request) {
        LOG.error("Unhandled exception type: {}", exception.getClass().getName());
        // Trả lỗi 500 an toàn
        return response(HttpStatus.INTERNAL_SERVER_ERROR, HttpHeaders.EMPTY,
                ErrorConstants.INTERNAL_ERROR, messageUtils.getMessage(ErrorConstants.INTERNAL_ERROR),
                List.of(), request);
    }

    private ResponseEntity<Object> response(
            HttpStatus status, HttpHeaders headers, String strCode, String message,
            List<FieldErrorResponse> fields, WebRequest request) {
        // Lấy đường dẫn request để FE biết request nào đã phát sinh lỗi.
        String path = request instanceof ServletWebRequest servletRequest
                ? servletRequest.getRequest().getRequestURI() : "";
        // Tạo chi tiết lỗi
        Instant timestamp = Instant.now();
        int httpStatus = status.value();
        ErrorDetailResponse detail = new ErrorDetailResponse(timestamp, httpStatus, path, fields);

        BaseResponse<ErrorDetailResponse> response = BaseResponse.error(
                StatusCode.from(status), strCode, message, detail);

        return new ResponseEntity<>(response, headers, status);
    }
}
