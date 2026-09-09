package tr.kopru.domain.caze.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import tr.kopru.domain.caze.MeasureType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateCaseRequest {
    @NotNull(message = "partnershipId zorunludur")
    private UUID partnershipId;

    @NotNull(message = "hastaRumuzu zorunludur")
    private String hastaRumuzu;

    private UUID patientId; // Opsiyonel (Klinik icin)

    @NotNull(message = "olcuTipi zorunludur (STL veya FIZIKSEL)")
    private MeasureType olcuTipi;

    private LocalDate teslimTarihi;
    private String genelNot;

    @NotEmpty(message = "En az bir vaka kalemi eklenmelidir")
    @Valid
    private List<CaseItemInputDto> items;
}
