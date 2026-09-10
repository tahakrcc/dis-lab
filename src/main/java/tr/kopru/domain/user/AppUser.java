package tr.kopru.domain.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "app_user")
@Getter
@Setter
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "kullanici_adi", nullable = false, unique = true)
    private String kullaniciAdi;

    @Column(nullable = false)
    private String ad;

    @Column(unique = true)
    private String email;

    private String telefon;

    @Column(name = "parola_hash", nullable = false)
    private String parolaHash;

    @Column(name = "is_super_admin", nullable = false)
    private boolean superAdmin = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
