package tr.kopru.domain.caze;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DentalCaseRepository extends JpaRepository<DentalCase, UUID> {

    @Query("SELECT c FROM DentalCase c " +
            "LEFT JOIN FETCH c.items i " +
            "LEFT JOIN FETCH i.serviceItem " +
            "WHERE c.id = :id")
    Optional<DentalCase> findByIdWithItems(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM DentalCase c WHERE c.id = :id")
    Optional<DentalCase> findByIdForUpdate(@Param("id") UUID id);

    // NOT: Tek sorguda "(:durum IS NULL OR ...)" kullanmak, durum NULL iken
    // Postgres'in enum (case_status) parametre tipini cozememesine yol acar
    // ("could not determine data type of parameter"). Bu yuzden turetilmis
    // sorgular kullanilip serviste dallandirilir.
    List<DentalCase> findAllByOrderByCreatedAtDesc();

    List<DentalCase> findByPartnership_IdOrderByCreatedAtDesc(UUID partnershipId);

    List<DentalCase> findByDurumOrderByCreatedAtDesc(CaseStatus durum);

    List<DentalCase> findByPartnership_IdAndDurumOrderByCreatedAtDesc(UUID partnershipId, CaseStatus durum);
}
