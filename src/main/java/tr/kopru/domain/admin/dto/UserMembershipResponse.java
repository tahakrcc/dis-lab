package tr.kopru.domain.admin.dto;

import lombok.Builder;
import lombok.Getter;
import tr.kopru.domain.org.OrgTipi;
import tr.kopru.domain.org.RolTipi;

import java.util.UUID;

@Getter
@Builder
public class UserMembershipResponse {
    private UUID orgId;
    private String orgAd;
    private OrgTipi orgTip;
    private RolTipi rol;
}
