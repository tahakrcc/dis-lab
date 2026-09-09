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

    @Query("SELECT c FROM DentalCase c " +
            "WHERE (:partnershipId IS NULL OR c.partnership.id = :partnershipId) " +
            "AND (:durum IS NULL OR c.durum = :durum) " +
            "ORDER BY c.createdAt DESC")
    List<DentalCase> findAllFiltered(
            @Param("partnershipId") UUID partnershipId,
            @Param("durum") CaseStatus durum);
}
