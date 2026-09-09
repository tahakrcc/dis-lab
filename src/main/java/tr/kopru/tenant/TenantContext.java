package tr.kopru.tenant;

import java.util.UUID;

public final class TenantContext {
    private static final ThreadLocal<UUID> CURRENT_USER = new ThreadLocal<>();
    private static final ThreadLocal<UUID> CURRENT_ORG = new ThreadLocal<>();

    private TenantContext() {}

    public static void setUserId(UUID userId) {
        CURRENT_USER.set(userId);
    }

    public static UUID getUserId() {
        return CURRENT_USER.get();
    }

    public static void setOrgId(UUID orgId) {
        CURRENT_ORG.set(orgId);
    }

    public static UUID getOrgId() {
        return CURRENT_ORG.get();
    }

    public static void clear() {
        CURRENT_USER.remove();
        CURRENT_ORG.remove();
    }
}
