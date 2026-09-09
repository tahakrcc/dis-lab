package tr.kopru.domain.org.dto;

import lombok.Builder;
import lombok.Getter;
import tr.kopru.common.Aktiflik;
import tr.kopru.domain.org.RolTipi;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class MemberResponse {
    private UUID id;
    private UUID userId;
    private String userAd;
    private String userEmail;
    private UUID orgId;
    private RolTipi rol;
    private Aktiflik durum;
    private OffsetDateTime createdAt;
}
