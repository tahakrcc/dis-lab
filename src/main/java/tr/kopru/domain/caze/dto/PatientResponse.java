package tr.kopru.domain.caze.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class PatientResponse {
    private UUID id;
    private UUID clinicId;
    private String ad;
    private String telefon;
    private LocalDate dogumTarihi;
    private String not;
    private OffsetDateTime createdAt;
}
