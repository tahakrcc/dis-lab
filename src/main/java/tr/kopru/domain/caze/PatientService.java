package tr.kopru.domain.caze;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.kopru.common.ApiException;
import tr.kopru.domain.caze.dto.CreatePatientRequest;
import tr.kopru.domain.caze.dto.PatientResponse;
import tr.kopru.domain.org.OrgTipi;
import tr.kopru.domain.org.Organization;
import tr.kopru.domain.org.OrganizationRepository;
import tr.kopru.tenant.TenantContext;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final OrganizationRepository organizationRepository;

    @Transactional
    public PatientResponse createPatient(CreatePatientRequest request) {
        UUID clinicId = TenantContext.getOrgId();
        if (clinicId == null) {
            throw ApiException.validationError("Aktif klinik organizasyonu secilmelidir (X-Org-Id).", null);
        }

        Organization clinic = organizationRepository.findById(clinicId)
                .orElseThrow(() -> ApiException.notFound("Klinik bulunamadi."));

        if (clinic.getTip() != OrgTipi.KLINIK) {
            throw ApiException.forbidden("Hasta kaydi sadece KLINIK tarafindan yapilabilir.");
        }

        Patient patient = new Patient();
        patient.setClinic(clinic);
        patient.setAd(request.getAd());
        patient.setTelefon(request.getTelefon());
        patient.setDogumTarihi(request.getDogumTarihi());
        patient.setNot(request.getNot());
        patient = patientRepository.save(patient);

        return mapToResponse(patient);
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> getPatients() {
        UUID clinicId = TenantContext.getOrgId();
        if (clinicId == null) {
            throw ApiException.validationError("Aktif klinik organizasyonu secilmelidir (X-Org-Id).", null);
        }

        return patientRepository.findAllByClinicId(clinicId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private PatientResponse mapToResponse(Patient p) {
        return PatientResponse.builder()
                .id(p.getId())
                .clinicId(p.getClinic().getId())
                .ad(p.getAd())
                .telefon(p.getTelefon())
                .dogumTarihi(p.getDogumTarihi())
                .not(p.getNot())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
