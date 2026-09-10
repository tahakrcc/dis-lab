package tr.kopru.domain.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class AdminUserResponse {
    private UUID id;
    private String kullaniciAdi;
    private String ad;
    private String email;
    private boolean superAdmin;
    private OffsetDateTime createdAt;
}
