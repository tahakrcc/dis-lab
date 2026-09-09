package tr.kopru.domain.caze.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class CaseTransitionRequest {
    @NotBlank(message = "aksiyon zorunludur")
    private String aksiyon;

    private Map<String, Object> payload;
}
