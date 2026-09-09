package tr.kopru.domain.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import tr.kopru.domain.catalog.BirimTipi;

@Getter
@Setter
public class CreateServiceItemRequest {
    @NotBlank(message = "Hizmet adi zorunludur")
    private String ad;

    @NotNull(message = "Birim tipi zorunludur (DIS veya ADET)")
    private BirimTipi birim;
}
