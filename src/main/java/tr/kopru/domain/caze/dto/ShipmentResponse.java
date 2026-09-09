package tr.kopru.domain.caze.dto;

import lombok.Builder;
import lombok.Getter;
import tr.kopru.domain.caze.ShipmentYon;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class ShipmentResponse {
    private UUID id;
    private UUID caseId;
    private ShipmentYon yon;
    private String kurye;
    private String takipNo;
    private OffsetDateTime gonderimAt;
    private OffsetDateTime teslimAt;
}
