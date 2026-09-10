package tr.kopru.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Aspect
@Component
// Transaction advice (order 0) DIŞ katman; bu aspect İÇ katman (order 100) olmalı
// ki set_config, işlemin gerçek bağlantısında çalışsın (aksi halde RLS bağlamı
// farklı/otomatik-commit bir bağlantıya yazılır ve okuma sorguları 0 satır döner).
@Order(100)
@RequiredArgsConstructor
public class TenantAspect {

    @PersistenceContext
    private final EntityManager entityManager;

    @Around("@within(org.springframework.transaction.annotation.Transactional) || @annotation(org.springframework.transaction.annotation.Transactional)")
    public Object applyTenantContext(ProceedingJoinPoint pjp) throws Throwable {
        UUID userId = TenantContext.getUserId();
        if (userId != null) {
            log.trace("Applying tenant app.user_id: {}", userId);
            entityManager.createNativeQuery("SELECT set_config('app.user_id', :uid, true)")
                    .setParameter("uid", userId.toString())
                    .getSingleResult();
        } else {
            log.trace("No user in TenantContext, clearing app.user_id");
            entityManager.createNativeQuery("SELECT set_config('app.user_id', '', true)")
                    .getSingleResult();
        }
        return pjp.proceed();
    }
}
