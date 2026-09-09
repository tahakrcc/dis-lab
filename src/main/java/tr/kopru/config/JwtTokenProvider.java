package tr.kopru.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final Duration accessTokenExpiration;
    private final Duration refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${kopru.jwt.secret}") String secret,
            @Value("${kopru.jwt.access-token-expiration:15m}") Duration accessTokenExpiration,
            @Value("${kopru.jwt.refresh-token-expiration:7d}") Duration refreshTokenExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String generateAccessToken(UUID userId, String kullaniciAdi) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiration.toMillis());
        return Jwts.builder()
                .subject(userId.toString())
                .claim("kullaniciAdi", kullaniciAdi)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(UUID userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpiration.toMillis());
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public UUID parseUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return UUID.fromString(claims.getSubject());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
