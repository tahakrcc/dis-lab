package tr.kopru.domain.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.kopru.common.ApiException;
import tr.kopru.common.Aktiflik;
import tr.kopru.config.JwtTokenProvider;
import tr.kopru.domain.org.Membership;
import tr.kopru.domain.org.MembershipRepository;
import tr.kopru.domain.user.dto.*;
import tr.kopru.tenant.TenantContext;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw ApiException.validationError("Bu email adresi zaten kayitli.", null);
        }

        AppUser user = new AppUser();
        user.setAd(request.getAd());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setTelefon(request.getTelefon());
        user.setParolaHash(passwordEncoder.encode(request.getParola()));
        user = userRepository.save(user);

        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> ApiException.unauthenticated("Email veya parola hatali."));

        if (!passwordEncoder.matches(request.getParola(), user.getParolaHash())) {
            throw ApiException.unauthenticated("Email veya parola hatali.");
        }

        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String rawToken = request.getRefresh();
        if (!jwtTokenProvider.validateToken(rawToken)) {
            throw ApiException.unauthenticated("Gecersiz refresh token.");
        }

        UUID userId = jwtTokenProvider.parseUserId(rawToken);
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> ApiException.unauthenticated("Gecersiz veya iptal edilmis refresh token."));

        if (refreshToken.getRevokedAt() != null || refreshToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw ApiException.unauthenticated("Refresh token suresi dolmus veya iptal edilmis.");
        }

        // Revoke old token and issue new pair
        refreshToken.setRevokedAt(OffsetDateTime.now());
        refreshTokenRepository.save(refreshToken);

        AppUser user = refreshToken.getUser();
        return createAuthResponse(user);
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        if (request != null && request.getRefresh() != null) {
            String tokenHash = hashToken(request.getRefresh());
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.setRevokedAt(OffsetDateTime.now());
                refreshTokenRepository.save(token);
            });
        }
    }

    @Transactional(readOnly = true)
    public UserMeResponse getMe() {
        UUID userId = TenantContext.getUserId();
        if (userId == null) {
            throw ApiException.unauthenticated("Kullanici oturum acmamis.");
        }

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));

        List<Membership> memberships = membershipRepository.findByUserIdAndDurumWithOrg(userId, Aktiflik.AKTIF);
        List<UserMeResponse.MembershipDto> membershipDtos = memberships.stream()
                .map(m -> UserMeResponse.MembershipDto.builder()
                        .orgId(m.getOrganization().getId())
                        .orgAd(m.getOrganization().getAd())
                        .orgTip(m.getOrganization().getTip())
                        .rol(m.getRol())
                        .build())
                .toList();

        return UserMeResponse.builder()
                .id(user.getId())
                .ad(user.getAd())
                .email(user.getEmail())
                .telefon(user.getTelefon())
                .memberships(membershipDtos)
                .build();
    }

    private AuthResponse createAuthResponse(AppUser user) {
        String access = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String refresh = jwtTokenProvider.generateRefreshToken(user.getId());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken(refresh));
        refreshToken.setExpiresAt(OffsetDateTime.now().plusDays(7));
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .access(access)
                .refresh(refresh)
                .build();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
