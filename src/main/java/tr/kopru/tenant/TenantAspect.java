package tr.kopru.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
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
