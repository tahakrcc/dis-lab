package tr.kopru.common;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex) {
        ApiError error = ApiError.of(ex.getCode(), ex.getMessage(), ex.getDetails());
        return new ResponseEntity<>(error, ex.getStatus());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "";
        if (msg.contains("idx_unique_case_borc") || msg.contains("unique_case_borc")) {
            ApiError error = ApiError.of(ErrorCode.ALREADY_DELIVERED, "Bu vaka için teslimat ve borç kaydı zaten oluşturulmuş.", null);
            return new ResponseEntity<>(error, HttpStatus.CONFLICT);
        }
        if (msg.contains("price_list_entry_no_overlap")) {
            ApiError error = ApiError.of(ErrorCode.VALIDATION_ERROR, "Tarih aralığı mevcut bir fiyat kaydı ile çakışıyor.", null);
            return new ResponseEntity<>(error, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        ApiError error = ApiError.of(ErrorCode.VALIDATION_ERROR, "Veri bütünlüğü ihlali: " + ex.getMostSpecificCause().getMessage(), null);
        return new ResponseEntity<>(error, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        ApiError error = ApiError.of(ErrorCode.VERSION_MISMATCH,
                "Vaka başka bir işlem tarafından güncellendi, lütfen tekrar deneyin.", null);
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        ApiError error = ApiError.of(ErrorCode.FORBIDDEN, "Bu işlem için yetkiniz bulunmamaktadır.", null);
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthException(AuthenticationException ex) {
        ApiError error = ApiError.of(ErrorCode.UNAUTHENTICATED, "Kimlik doğrulama başarısız.", null);
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        Map<String, Object> details = new HashMap<>();
        for (FieldError err : ex.getBindingResult().getFieldErrors()) {
            details.put(err.getField(), err.getDefaultMessage());
        }
        ApiError error = ApiError.of(ErrorCode.VALIDATION_ERROR, "Doğrulama hatası", details);
        return new ResponseEntity<>(error, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(Exception ex) {
        ApiError error = ApiError.of(ErrorCode.VALIDATION_ERROR, ex.getMessage(), null);
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
