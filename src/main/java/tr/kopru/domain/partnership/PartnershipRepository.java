package tr.kopru.domain.partnership;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PartnershipRepository extends JpaRepository<Partnership, UUID> {

    @Query("SELECT p FROM Partnership p JOIN FETCH p.lab JOIN FETCH p.clinic WHERE p.id = :id")
    Optional<Partnership> findByIdWithOrgs(@Param("id") UUID id);

    @Query("SELECT p FROM Partnership p JOIN FETCH p.lab JOIN FETCH p.clinic WHERE p.lab.id = :orgId OR p.clinic.id = :orgId")
    List<Partnership> findAllByOrgId(@Param("orgId") UUID orgId);

    Optional<Partnership> findByLabIdAndClinicId(UUID labId, UUID clinicId);
}
