package tr.kopru.domain.partnership.dto;

import lombok.Builder;
import lombok.Getter;
import tr.kopru.common.Aktiflik;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class PartnershipResponse {
    private UUID id;
    private UUID labId;
    private String labAd;
    private UUID clinicId;
    private String clinicAd;
    private Aktiflik durum;
    private int vadeGun;
    private OffsetDateTime createdAt;
}
