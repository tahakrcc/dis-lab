package tr.kopru.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Getter
public class ApiException extends RuntimeException {
    private final ErrorCode code;
    private final HttpStatus status;
    private final Map<String, Object> details;

    public ApiException(ErrorCode code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = null;
    }

    public ApiException(ErrorCode code, String message, HttpStatus status, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = details;
    }

    public static ApiException unauthenticated(String message) {
        return new ApiException(ErrorCode.UNAUTHENTICATED, message, HttpStatus.UNAUTHORIZED);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(ErrorCode.FORBIDDEN, message, HttpStatus.FORBIDDEN);
    }

    public static ApiException notFound(String message) {
        return new ApiException(ErrorCode.NOT_FOUND, message, HttpStatus.NOT_FOUND);
    }

    public static ApiException invalidTransition(String message) {
        return new ApiException(ErrorCode.INVALID_TRANSITION, message, HttpStatus.CONFLICT);
    }

    public static ApiException alreadyDelivered(String message) {
        return new ApiException(ErrorCode.ALREADY_DELIVERED, message, HttpStatus.CONFLICT);
    }

    public static ApiException versionMismatch(String message) {
        return new ApiException(ErrorCode.VERSION_MISMATCH, message, HttpStatus.CONFLICT);
    }

    public static ApiException priceNotFound(String message, Map<String, Object> details) {
        return new ApiException(ErrorCode.PRICE_NOT_FOUND, message, HttpStatus.UNPROCESSABLE_ENTITY, details);
    }

    public static ApiException validationError(String message, Map<String, Object> details) {
        return new ApiException(ErrorCode.VALIDATION_ERROR, message, HttpStatus.UNPROCESSABLE_ENTITY, details);
    }
}
