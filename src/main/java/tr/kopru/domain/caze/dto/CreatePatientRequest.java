package tr.kopru.domain.caze.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreatePatientRequest {
    @NotBlank(message = "Hasta adi zorunludur")
    private String ad;

    private String telefon;
    private LocalDate dogumTarihi;
    private String not;
}
