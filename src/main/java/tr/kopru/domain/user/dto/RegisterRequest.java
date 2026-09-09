package tr.kopru.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    @NotBlank(message = "Ad alani zorunludur")
    private String ad;

    @NotBlank(message = "Email alani zorunludur")
    @Email(message = "Gecerli bir email adresi giriniz")
    private String email;

    private String telefon;

    @NotBlank(message = "Parola alani zorunludur")
    private String parola;
}
