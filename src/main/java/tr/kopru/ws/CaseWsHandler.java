package tr.kopru.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vaka bazli ham WebSocket handler. Her baglanti bir vakaya (caseId) abonedir;
 * o vakada bir olay olunca (mesaj / durum / ek) o odadaki tum oturumlara JSON gonderir.
 * Yetki handshake'te (WsHandshakeInterceptor) RLS ile dogrulanir.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CaseWsHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final Map<UUID, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        UUID caseId = (UUID) session.getAttributes().get("caseId");
        if (caseId == null) return;
        rooms.computeIfAbsent(caseId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID caseId = (UUID) session.getAttributes().get("caseId");
        if (caseId == null) return;
        Set<WebSocketSession> set = rooms.get(caseId);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) rooms.remove(caseId);
        }
    }

    /** Bir vakadaki tum abonelere olay yayinla. Aktif transaction varsa COMMIT
     *  sonrasina ertelenir; aksi halde alici, commit'ten once tazeleyip yeni mesaji kaciriyor. */
    public void broadcast(UUID caseId, Object payload) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doSend(caseId, payload);
                }
            });
        } else {
            doSend(caseId, payload);
        }
    }

    private void doSend(UUID caseId, Object payload) {
        Set<WebSocketSession> set = rooms.get(caseId);
        if (set == null || set.isEmpty()) return;
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return;
        }
        TextMessage msg = new TextMessage(json);
        for (WebSocketSession s : set) {
            if (!s.isOpen()) continue;
            try {
                synchronized (s) {
                    s.sendMessage(msg);
                }
            } catch (Exception e) {
                log.debug("WS gonderim hatasi: {}", e.getMessage());
            }
        }
    }
}
