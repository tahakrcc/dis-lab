package tr.kopru.domain.partnership;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tr.kopru.common.Aktiflik;
import tr.kopru.domain.org.Organization;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "partnership", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"lab_id", "clinic_id"})
})
@Getter
@Setter
public class Partnership {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_id", nullable = false)
    private Organization lab;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Organization clinic;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "aktiflik")
    private Aktiflik durum = Aktiflik.AKTIF;

    @Column(name = "vade_gun", nullable = false)
    private int vadeGun = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
