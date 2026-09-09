package tr.kopru.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
    private ErrorDetail error;

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {
        private ErrorCode code;
        private String message;
        private Map<String, Object> details;
    }

    public static ApiError of(ErrorCode code, String message, Map<String, Object> details) {
        return ApiError.builder()
                .error(ErrorDetail.builder()
                        .code(code)
                        .message(message)
                        .details(details != null && !details.isEmpty() ? details : null)
                        .build())
                .build();
    }
}
