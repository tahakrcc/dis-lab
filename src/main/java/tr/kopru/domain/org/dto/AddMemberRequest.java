package tr.kopru.domain.org.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import tr.kopru.domain.org.RolTipi;

@Getter
@Setter
public class AddMemberRequest {
    @NotBlank(message = "Email zorunludur")
    @Email(message = "Gecerli bir email adresi giriniz")
    private String email;

    @NotNull(message = "Rol zorunludur")
    private RolTipi rol;
}
