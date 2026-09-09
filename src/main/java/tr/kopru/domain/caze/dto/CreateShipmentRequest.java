package tr.kopru.domain.caze.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import tr.kopru.domain.caze.ShipmentYon;

import java.time.OffsetDateTime;

@Getter
@Setter
public class CreateShipmentRequest {
    @NotNull(message = "yon zorunludur (KLINIKTEN_LABA veya LABDAN_KLINIGE)")
    private ShipmentYon yon;

    private String kurye;
    private String takipNo;
    private OffsetDateTime gonderimAt;
}
