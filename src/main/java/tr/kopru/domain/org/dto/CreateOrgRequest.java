package tr.kopru.domain.org.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import tr.kopru.domain.org.OrgTipi;

@Getter
@Setter
public class CreateOrgRequest {
    @NotNull(message = "Tip zorunludur (LAB veya KLINIK)")
    private OrgTipi tip;

    @NotBlank(message = "Organizasyon adi zorunludur")
    private String ad;

    private String telefon;
    private String vergiNo;
    private String adres;
    private String ayarlar;
}
