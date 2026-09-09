package tr.kopru.domain.partnership.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreatePartnershipRequest {
    @NotNull(message = "labId zorunludur")
    private UUID labId;

    @NotNull(message = "clinicId zorunludur")
    private UUID clinicId;

    private Integer vadeGun = 0;
}
