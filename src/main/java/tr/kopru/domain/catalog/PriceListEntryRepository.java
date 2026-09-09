package tr.kopru.domain.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PriceListEntryRepository extends JpaRepository<PriceListEntry, UUID> {

    @Query("SELECT ple FROM PriceListEntry ple JOIN FETCH ple.serviceItem " +
            "WHERE ple.partnership.id = :partnershipId AND ple.aktif = true")
    List<PriceListEntry> findAllActiveByPartnershipId(@Param("partnershipId") UUID partnershipId);

    @Query("SELECT ple FROM PriceListEntry ple " +
            "WHERE ple.partnership.id = :partnershipId " +
            "AND ple.serviceItem.id = :serviceItemId " +
            "AND ple.aktif = true " +
            "AND ple.gecerliBaslangic <= :targetDate " +
            "AND (ple.gecerliBitis IS NULL OR ple.gecerliBitis > :targetDate)")
    Optional<PriceListEntry> findValidPrice(
            @Param("partnershipId") UUID partnershipId,
            @Param("serviceItemId") UUID serviceItemId,
            @Param("targetDate") LocalDate targetDate);

    @Query("SELECT ple FROM PriceListEntry ple " +
            "WHERE ple.partnership.id = :partnershipId " +
            "AND ple.serviceItem.id = :serviceItemId " +
            "AND ple.aktif = true " +
            "AND (ple.gecerliBitis IS NULL OR ple.gecerliBitis > :targetDate)")
    List<PriceListEntry> findOverlappingActiveEntries(
            @Param("partnershipId") UUID partnershipId,
            @Param("serviceItemId") UUID serviceItemId,
            @Param("targetDate") LocalDate targetDate);
}
