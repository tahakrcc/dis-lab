package tr.kopru.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "Kullanici adi alani zorunludur")
    private String kullaniciAdi;

    @NotBlank(message = "Parola alani zorunludur")
    private String parola;
}
