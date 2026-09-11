package tr.kopru.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import tr.kopru.config.JwtTokenProvider;
import tr.kopru.domain.caze.CaseAccessService;
import tr.kopru.tenant.TenantContext;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * WebSocket handshake dogrulamasi. Tarayici WebSocket'i Authorization header
 * gonderemedigi icin JWT query param (?token=) ile gelir; ayrica ?caseId=
 * belirtilir. Token gecerliyse ve kullanici o vakayi (RLS) gorebiliyorsa
 * baglantiya izin verilir; userId + caseId oturum ozniteliklerine yazilir.
 */
@Component
@RequiredArgsConstructor
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwt;
    private final CaseAccessService caseAccessService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Map<String, String> q = parseQuery(request.getURI());
        String token = q.get("token");
        String caseIdStr = q.get("caseId");
        if (token == null || caseIdStr == null || !jwt.validateToken(token)) return false;

        UUID userId;
        UUID caseId;
        try {
            userId = jwt.parseUserId(token);
            caseId = UUID.fromString(caseIdStr);
        } catch (Exception e) {
            return false;
        }

        TenantContext.setUserId(userId);
        try {
            if (!caseAccessService.canAccessCase(caseId)) return false;
        } finally {
            TenantContext.clear();
        }

        attributes.put("userId", userId);
        attributes.put("caseId", caseId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    private static Map<String, String> parseQuery(URI uri) {
        Map<String, String> m = new HashMap<>();
        String q = uri.getQuery();
        if (q != null) {
            for (String p : q.split("&")) {
                int i = p.indexOf('=');
                if (i > 0) {
                    m.put(p.substring(0, i), URLDecoder.decode(p.substring(i + 1), StandardCharsets.UTF_8));
                }
            }
        }
        return m;
    }
}
