package com.luvina.base.core.exception;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.luvina.base.core.dto.ApiError;
import com.luvina.base.core.i18n.ErrorCode;
import com.luvina.base.core.i18n.MessageUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Translates exceptions into the single {@link ApiError} contract.
 *
 * <p>Two rules govern this class:
 * <ul>
 *   <li>every response body has the same shape, whatever went wrong;</li>
 *   <li>internal exception text never reaches the client. Unexpected failures are
 *       logged with their stack trace and reported as a generic message.</li>
 * </ul>
 *
 * <p>Security failures are handled separately by {@code common-security}, which
 * owns the Spring Security types.
 */
@Slf4j
// Lowest precedence: an advice with a more specific handler, such as the one in
// common-security, must be given the exception first.
@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageUtil messageUtil;

    /**
     * Handles every expected business failure. Status and code come from the
     * exception itself.
     *
     * @param ex      the failure
     * @param request current request, used for the path field
     * @return error response
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest request) {
        log.debug("Business failure [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    /**
     * Handles failed validation of a validated request body.
     *
     * @param ex      the validation failure
     * @param request current request
     * @return error response carrying one entry per rejected field
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException ex,
                                                        HttpServletRequest request) {
        List<ApiError.FieldErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toDetail)
                .toList();
        return build(HttpStatus.BAD_REQUEST, ErrorCode.ERR014,
                messageUtil.getMessage(ErrorCode.ERR014), request, details);
    }

    /**
     * Handles failed validation of method parameters and of objects checked
     * through {@link com.luvina.base.core.validation.ValidatorWrapper}.
     *
     * @param ex      the validation failure
     * @param request current request
     * @return error response carrying one entry per violation
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex,
                                                             HttpServletRequest request) {
        List<ApiError.FieldErrorDetail> details = ex.getConstraintViolations().stream()
                .map(this::toDetail)
                .toList();
        return build(HttpStatus.BAD_REQUEST, ErrorCode.ERR014,
                messageUtil.getMessage(ErrorCode.ERR014), request, details);
    }

    /**
     * Handles a query parameter or path variable that cannot be converted to the
     * declared type, for example a malformed UUID.
     *
     * @param ex      the conversion failure
     * @param request current request
     * @return error response
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                       HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.ERR017,
                messageUtil.getMessage(ErrorCode.ERR017, ex.getName()), request, null);
    }

    /**
     * Handles a request body that is absent or not valid JSON.
     *
     * @param ex      the parse failure
     * @param request current request
     * @return error response
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                        HttpServletRequest request) {
        log.debug("Unreadable request body: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ErrorCode.ERR017,
                messageUtil.getMessage(ErrorCode.ERR014), request, null);
    }

    /**
     * Handles an upload larger than the configured multipart limit.
     *
     * @param ex      the size failure
     * @param request current request
     * @return error response
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleUploadTooLarge(MaxUploadSizeExceededException ex,
                                                        HttpServletRequest request) {
        log.debug("Upload rejected: {}", ex.getMessage());
        return build(HttpStatus.PAYLOAD_TOO_LARGE, ErrorCode.ERR023,
                messageUtil.getMessage(ErrorCode.ERR023, ex.getMaxUploadSize()), request, null);
    }

    /**
     * Handles a database constraint the service layer did not anticipate,
     * typically a unique index. Reported as a conflict rather than a 500 because
     * the caller can fix it by changing the payload.
     *
     * @param ex      the constraint failure
     * @param request current request
     * @return error response
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex,
                                                       HttpServletRequest request) {
        log.warn("Database constraint violated on {}", request.getRequestURI(), ex);
        return build(HttpStatus.CONFLICT, ErrorCode.ERR002,
                messageUtil.getMessage(ErrorCode.ERR012), request, null);
    }

    /**
     * Last-resort handler. Logs the stack trace and answers with a generic
     * message so that no internal detail leaks to the client.
     *
     * @param ex      the failure
     * @param request current request
     * @return error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.ERR015,
                messageUtil.getMessage(ErrorCode.ERR015), request, null);
    }

    private ApiError.FieldErrorDetail toDetail(FieldError error) {
        return new ApiError.FieldErrorDetail(error.getField(), error.getDefaultMessage());
    }

    private ApiError.FieldErrorDetail toDetail(ConstraintViolation<?> violation) {
        return new ApiError.FieldErrorDetail(
                violation.getPropertyPath().toString(), violation.getMessage());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, ErrorCode code, String message,
                                          HttpServletRequest request,
                                          List<ApiError.FieldErrorDetail> fieldErrors) {
        ApiError body = ApiError.builder()
                .timestamp(OffsetDateTime.now())
                .status(status.value())
                .code(code.name())
                .message(message)
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
