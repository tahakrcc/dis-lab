package tr.kopru.domain.catalog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import tr.kopru.domain.partnership.Partnership;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "price_list_entry")
@Getter
@Setter
public class PriceListEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partnership_id", nullable = false)
    private Partnership partnership;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_item_id", nullable = false)
    private ServiceItem serviceItem;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal fiyat;

    @Column(name = "gecerli_baslangic", nullable = false)
    private LocalDate gecerliBaslangic;

    @Column(name = "gecerli_bitis")
    private LocalDate gecerliBitis;

    @Column(nullable = false)
    private boolean aktif = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
