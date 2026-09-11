package tr.kopru.domain.attachment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tr.kopru.domain.attachment.dto.AttachmentResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping("/cases/{caseId}/attachments")
    public ResponseEntity<List<AttachmentResponse>> list(@PathVariable UUID caseId) {
        return ResponseEntity.ok(attachmentService.list(caseId));
    }

    @PostMapping("/cases/{caseId}/attachments")
    public ResponseEntity<AttachmentResponse> upload(
            @PathVariable UUID caseId, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attachmentService.upload(caseId, file));
    }

    @GetMapping("/attachments/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        Attachment a = attachmentService.download(id);
        MediaType mediaType;
        try {
            mediaType = a.getMime() != null ? MediaType.parseMediaType(a.getMime()) : MediaType.APPLICATION_OCTET_STREAM;
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + a.getDosyaAdi() + "\"")
                .body(a.getIcerik());
    }
}
