package tr.kopru.domain.partnership.dto;

import lombok.Getter;
import lombok.Setter;
import tr.kopru.common.Aktiflik;

@Getter
@Setter
public class UpdatePartnershipRequest {
    private Aktiflik durum;
    private Integer vadeGun;
}
