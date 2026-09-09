package tr.kopru.domain.user.dto;

import lombok.Builder;
import lombok.Getter;
import tr.kopru.domain.org.OrgTipi;
import tr.kopru.domain.org.RolTipi;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class UserMeResponse {
    private UUID id;
    private String kullaniciAdi;
    private String ad;
    private String email;
    private String telefon;
    private List<MembershipDto> memberships;

    @Getter
    @Builder
    public static class MembershipDto {
        private UUID orgId;
        private String orgAd;
        private OrgTipi orgTip;
        private RolTipi rol;
    }
}
