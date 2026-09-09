package tr.kopru.domain.org.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import tr.kopru.domain.org.RolTipi;

@Getter
@Setter
public class AddMemberRequest {
    @NotBlank(message = "Kullanici adi zorunludur")
    private String kullaniciAdi;

    @NotNull(message = "Rol zorunludur")
    private RolTipi rol;
}
