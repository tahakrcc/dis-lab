package tr.kopru.domain.attachment;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tr.kopru.common.ApiException;
import tr.kopru.domain.attachment.dto.AttachmentResponse;
import tr.kopru.domain.org.Organization;
import tr.kopru.domain.org.OrganizationRepository;
import tr.kopru.domain.user.AppUser;
import tr.kopru.domain.user.AppUserRepository;
import tr.kopru.tenant.TenantContext;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final AppUserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(UUID caseId) {
        return attachmentRepository.listByCaseId(caseId);
    }

    @Transactional
    public AttachmentResponse upload(UUID caseId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.validationError("Dosya bos olamaz.", null);
        }
        UUID userId = TenantContext.getUserId();
        UUID orgId = TenantContext.getOrgId();
        if (userId == null || orgId == null) {
            throw ApiException.validationError("Dosya icin aktif organizasyon (X-Org-Id) gereklidir.", null);
        }
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> ApiException.notFound("Organizasyon bulunamadi."));

        Attachment a = new Attachment();
        a.setCaseId(caseId);
        a.setUploaderUserId(userId);
        a.setUploaderAd(user.getAd());
        a.setUploaderTaraf(org.getTip());
        a.setDosyaAdi(file.getOriginalFilename() != null ? file.getOriginalFilename() : "dosya");
        a.setMime(file.getContentType());
        a.setBoyut(file.getSize());
        try {
            a.setIcerik(file.getBytes());
        } catch (IOException e) {
            throw ApiException.validationError("Dosya okunamadi.", null);
        }

        try {
            a = attachmentRepository.save(a);
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.forbidden("Bu vakaya dosya ekleme yetkiniz yok.");
        }
        return map(a);
    }

    @Transactional(readOnly = true)
    public Attachment download(UUID id) {
        return attachmentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Dosya bulunamadi."));
    }

    private AttachmentResponse map(Attachment a) {
        return new AttachmentResponse(a.getId(), a.getCaseId(), a.getUploaderUserId(), a.getUploaderAd(),
                a.getUploaderTaraf(), a.getDosyaAdi(), a.getMime(), a.getBoyut(), a.getCreatedAt());
    }
}
