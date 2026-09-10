package tr.kopru.domain.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    @Query("SELECT l FROM LedgerEntry l " +
            "WHERE l.partnership.id = :partnershipId " +
            "AND (:from IS NULL OR l.belgeTarihi >= :from) " +
            "AND (:to IS NULL OR l.belgeTarihi <= :to) " +
            "ORDER BY l.belgeTarihi ASC, l.createdAt ASC")
    List<LedgerEntry> findByPartnershipIdAndDateRange(
            @Param("partnershipId") UUID partnershipId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    // NOT: Cok kolonlu native sorguda Optional<Object[]> belirsizdir (Spring Data
    // diziyi "sonuc listesi" gibi yorumlayip tek-eleman dondurebiliyor -> "Index 1
    // out of bounds"). List<Object[]> ile her satir dogru sekilde Object[4] olur.
    @Query(value = "SELECT partnership_id, toplam_borc, toplam_tahsilat, bakiye " +
            "FROM balance WHERE partnership_id = :partnershipId", nativeQuery = true)
    List<Object[]> getBalanceNative(@Param("partnershipId") UUID partnershipId);
}
