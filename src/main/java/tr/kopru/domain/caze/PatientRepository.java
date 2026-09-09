package tr.kopru.domain.caze;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    List<Patient> findAllByClinicId(UUID clinicId);
    Optional<Patient> findByIdAndClinicId(UUID id, UUID clinicId);
}
