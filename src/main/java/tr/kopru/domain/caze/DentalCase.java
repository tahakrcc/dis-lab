package tr.kopru.domain.caze;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import tr.kopru.domain.partnership.Partnership;
import tr.kopru.domain.user.AppUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "\"case\"", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"partnership_id", "kod"})
})
@Getter
@Setter
public class DentalCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partnership_id", nullable = false)
    private Partnership partnership;

    @Column(nullable = false)
    private String kod;

    @Column(name = "hasta_rumuzu", nullable = false)
    private String hastaRumuzu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private AppUser assignedTo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "olcu_tipi", nullable = false, columnDefinition = "measure_type")
    private MeasureType olcuTipi;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "case_status")
    private CaseStatus durum = CaseStatus.TASLAK;

    @Column(name = "teslim_tarihi")
    private LocalDate teslimTarihi;

    @Column(name = "toplam_tutar", nullable = false, precision = 12, scale = 2)
    private BigDecimal toplamTutar = BigDecimal.ZERO;

    @Column(name = "fiyat_versiyon", nullable = false)
    private int fiyatVersiyon = 1;

    @Column(name = "onay_klinik_versiyon")
    private Integer onayKlinikVersiyon;

    @Column(name = "onay_lab_versiyon")
    private Integer onayLabVersiyon;

    @Column(name = "onay_klinik_at")
    private OffsetDateTime onayKlinikAt;

    @Column(name = "onay_lab_at")
    private OffsetDateTime onayLabAt;

    @Column(name = "revizyon_sayisi", nullable = false)
    private int revizyonSayisi = 0;

    @Column(name = "iptal_neden")
    private String iptalNeden;

    @Column(name = "genel_not")
    private String genelNot;

    @OneToMany(mappedBy = "dentalCase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CaseItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
