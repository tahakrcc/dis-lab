package tr.kopru.domain.message;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.kopru.common.ApiException;
import tr.kopru.domain.message.dto.CreateMessageRequest;
import tr.kopru.domain.message.dto.MessageResponse;
import tr.kopru.domain.org.Organization;
import tr.kopru.domain.org.OrganizationRepository;
import tr.kopru.domain.user.AppUser;
import tr.kopru.domain.user.AppUserRepository;
import tr.kopru.tenant.TenantContext;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final AppUserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final tr.kopru.ws.CaseWsHandler caseWsHandler;

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(UUID caseId) {
        // RLS: yalnizca ortakligin uyeleri gorur
        return messageRepository.findByCaseIdOrderByCreatedAtAsc(caseId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    /** Karşı tarafın mesajlarını "okundu" işaretle (mevcut kullanıcı okuyor). */
    @Transactional
    public void markRead(UUID caseId) {
        UUID userId = TenantContext.getUserId();
        if (userId == null) return;
        messageRepository.markOthersRead(caseId, userId);
    }

    @Transactional
    public MessageResponse sendMessage(UUID caseId, CreateMessageRequest request) {
        UUID userId = TenantContext.getUserId();
        UUID orgId = TenantContext.getOrgId();
        if (userId == null || orgId == null) {
            throw ApiException.validationError("Mesaj icin aktif organizasyon (X-Org-Id) gereklidir.", null);
        }

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> ApiException.notFound("Organizasyon bulunamadi."));

        Message m = new Message();
        m.setCaseId(caseId);
        m.setSenderUserId(userId);
        m.setSenderAd(user.getAd());
        m.setSenderTaraf(org.getTip());
        m.setMetin(request.getMetin().trim());

        try {
            m = messageRepository.save(m);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // RLS WITH CHECK ihlali / gecersiz case
            throw ApiException.forbidden("Bu vakaya mesaj gonderme yetkiniz yok.");
        }

        caseWsHandler.broadcast(caseId, java.util.Map.of("type", "message", "caseId", caseId.toString()));

        return mapToResponse(m);
    }

    /** Yalnizca gonderen kendi mesajini duzenleyebilir. */
    @Transactional
    public MessageResponse editMessage(UUID caseId, UUID messageId, String metin) {
        UUID userId = TenantContext.getUserId();
        Message m = messageRepository.findById(messageId)
                .orElseThrow(() -> ApiException.notFound("Mesaj bulunamadi."));
        if (!m.getCaseId().equals(caseId)) {
            throw ApiException.notFound("Mesaj bulunamadi.");
        }
        if (userId == null || !m.getSenderUserId().equals(userId)) {
            throw ApiException.forbidden("Yalnizca kendi mesajinizi duzenleyebilirsiniz.");
        }
        if (m.getSilindiAt() != null) {
            throw ApiException.validationError("Silinmis mesaj duzenlenemez.", null);
        }
        m.setMetin(metin.trim());
        m.setDuzenlendiAt(OffsetDateTime.now());
        m = messageRepository.save(m);
        caseWsHandler.broadcast(caseId, java.util.Map.of("type", "message", "caseId", caseId.toString()));
        return mapToResponse(m);
    }

    /** Yumusak silme: icerik gizlenir, kayit kalir. Yalnizca gonderen silebilir. */
    @Transactional
    public void deleteMessage(UUID caseId, UUID messageId) {
        UUID userId = TenantContext.getUserId();
        Message m = messageRepository.findById(messageId)
                .orElseThrow(() -> ApiException.notFound("Mesaj bulunamadi."));
        if (!m.getCaseId().equals(caseId)) {
            throw ApiException.notFound("Mesaj bulunamadi.");
        }
        if (userId == null || !m.getSenderUserId().equals(userId)) {
            throw ApiException.forbidden("Yalnizca kendi mesajinizi silebilirsiniz.");
        }
        if (m.getSilindiAt() == null) {
            m.setSilindiAt(OffsetDateTime.now());
            messageRepository.save(m);
            caseWsHandler.broadcast(caseId, java.util.Map.of("type", "message", "caseId", caseId.toString()));
        }
    }

    private MessageResponse mapToResponse(Message m) {
        return MessageResponse.builder()
                .id(m.getId())
                .caseId(m.getCaseId())
                .senderUserId(m.getSenderUserId())
                .senderAd(m.getSenderAd())
                .senderTaraf(m.getSenderTaraf())
                .metin(m.getSilindiAt() != null ? null : m.getMetin())
                .okunduAt(m.getOkunduAt())
                .duzenlendiAt(m.getDuzenlendiAt())
                .silindiAt(m.getSilindiAt())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
