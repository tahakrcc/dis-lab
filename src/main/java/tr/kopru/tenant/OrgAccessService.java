package tr.kopru.tenant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.kopru.common.Aktiflik;
import tr.kopru.domain.org.MembershipRepository;
import tr.kopru.domain.org.OrgTipi;
import tr.kopru.domain.org.RolTipi;

import java.util.Optional;
import java.util.UUID;

/**
 * X-Org-Id üyelik doğrulaması. @Transactional olmasi kritik: boylece TenantAspect
 * islemin ICINDE calisip app.user_id'yi set eder ve RLS altinda kullanicinin KENDI
 * uyeligi gorunur. (Filtreden dogrudan repository cagrisi islem disinda kaldigi icin
 * RLS 0 satir donduruyordu.)
 */
@Service
@RequiredArgsConstructor
public class OrgAccessService {

    private final MembershipRepository membershipRepository;

    public record OrgAuth(RolTipi rol, OrgTipi orgTip) {}

    @Transactional(readOnly = true)
    public Optional<OrgAuth> resolveActiveMembership(UUID userId, UUID orgId) {
        return membershipRepository
                .findByUserIdAndOrganizationIdAndDurum(userId, orgId, Aktiflik.AKTIF)
                .map(m -> new OrgAuth(m.getRol(), m.getOrganization().getTip()));
    }
}
