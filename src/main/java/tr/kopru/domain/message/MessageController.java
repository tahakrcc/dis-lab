package tr.kopru.domain.message;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tr.kopru.domain.message.dto.CreateMessageRequest;
import tr.kopru.domain.message.dto.MessageResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cases/{caseId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<List<MessageResponse>> getMessages(@PathVariable UUID caseId) {
        return ResponseEntity.ok(messageService.getMessages(caseId));
    }

    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable UUID caseId, @Valid @RequestBody CreateMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(messageService.sendMessage(caseId, request));
    }

    @PostMapping("/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID caseId) {
        messageService.markRead(caseId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{messageId}")
    public ResponseEntity<MessageResponse> edit(
            @PathVariable UUID caseId, @PathVariable UUID messageId,
            @Valid @RequestBody CreateMessageRequest request) {
        return ResponseEntity.ok(messageService.editMessage(caseId, messageId, request.getMetin()));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> delete(@PathVariable UUID caseId, @PathVariable UUID messageId) {
        messageService.deleteMessage(caseId, messageId);
        return ResponseEntity.noContent().build();
    }
}
