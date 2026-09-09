package tr.kopru.domain.caze.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class UpdateShipmentRequest {
    private OffsetDateTime gonderimAt;
    private OffsetDateTime teslimAt;
}
