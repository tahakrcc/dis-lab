package tr.kopru.domain.caze;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * WebSocket abonelik yetkisi icin: @Transactional oldugundan TenantAspect
 * app.user_id'yi set eder ve existsById RLS altinda calisir -> kullanici yalnizca
 * kendi ortakligindaki vakaya abone olabilir.
 */
@Service
@RequiredArgsConstructor
public class CaseAccessService {

    private final DentalCaseRepository caseRepository;

    @Transactional(readOnly = true)
    public boolean canAccessCase(UUID caseId) {
        return caseRepository.existsById(caseId);
    }
}
