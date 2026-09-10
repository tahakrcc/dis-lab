package tr.kopru.domain.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserRequest {
    @NotBlank(message = "Kullanici adi zorunludur")
    private String kullaniciAdi;

    @NotBlank(message = "Ad zorunludur")
    private String ad;

    @NotBlank(message = "Parola zorunludur")
    private String parola;

    @Email(message = "Gecerli bir email adresi giriniz")
    private String email;
}
