package tr.kopru.domain.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateMessageRequest {
    @NotBlank(message = "Mesaj bos olamaz")
    @Size(max = 4000, message = "Mesaj en fazla 4000 karakter olabilir")
    private String metin;
}
