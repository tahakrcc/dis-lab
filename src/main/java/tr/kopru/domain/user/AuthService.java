package tr.kopru.domain.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        AppUser user = new AppUser();
        user.setKullaniciAdi(request.getKullaniciAdi().toLowerCase().trim());
        user.setAd(request.getAd());
        user.setEmail(request.getEmail() != null && !request.getEmail().isBlank()
                ? request.getEmail().toLowerCase().trim() : null);
        user.setTelefon(request.getTelefon());
        user.setParolaHash(passwordEncoder.encode(request.getParola()));

        try {
            // saveAndFlush => unique ihlali burada yakalanir (RLS altinda existsBy* guvenilmez)
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.validationError("Bu kullanici adi veya email zaten kayitli.", null);
        }

        // Yeni kullanicinin refresh token'ini yazabilmek icin RLS baglamini kur
        bindTenant(user.getId());
        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // 1) Pre-auth lookup (SECURITY DEFINER) — app.user_id henuz bos
        List<Object[]> rows = userRepository.findLoginRaw(request.getKullaniciAdi().toLowerCase().trim());
        if (rows.isEmpty()) {
            throw ApiException.unauthenticated("Kullanici adi veya parola hatali.");
        }
        Object[] row = rows.get(0);
        UUID userId = (UUID) row[0];
        String parolaHash = (String) row[1];

        // 2) Parola dogrula
        if (!passwordEncoder.matches(request.getParola(), parolaHash)) {
            throw ApiException.unauthenticated("Kullanici adi veya parola hatali.");
        }

        // 3) Artik kimlik dogru; RLS baglamini kur ve tam kullaniciyi yukle
        bindTenant(userId);
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthenticated("Kullanici adi veya parola hatali."));

        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String rawToken = request.getRefresh();
        if (!jwtTokenProvider.validateToken(rawToken)) {
            throw ApiException.unauthenticated("Gecersiz refresh token.");
        }

        UUID userId = jwtTokenProvider.parseUserId(rawToken);
        // refresh_token ve app_user'a RLS altinda erisebilmek icin baglami kur
        bindTenant(userId);

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
            if (jwtTokenProvider.validateToken(request.getRefresh())) {
                bindTenant(jwtTokenProvider.parseUserId(request.getRefresh()));
            }
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
                .kullaniciAdi(user.getKullaniciAdi())
                .ad(user.getAd())
                .email(user.getEmail())
                .telefon(user.getTelefon())
                .superAdmin(user.isSuperAdmin())
                .memberships(membershipDtos)
                .build();
    }

    /**
     * Transaction icinde app.user_id'yi set eder. TenantAspect metot girisinde bunu
     * bos ('') olarak ayarlar; auth akisinda kimlik belirlendikten sonra RLS'in dogru
     * calismasi (refresh_token insert WITH CHECK, app_user SELECT) icin gereklidir.
     */
    private void bindTenant(UUID userId) {
        TenantContext.setUserId(userId);
        entityManager.createNativeQuery("SELECT set_config('app.user_id', :uid, true)")
                .setParameter("uid", userId.toString())
                .getSingleResult();
    }

    private AuthResponse createAuthResponse(AppUser user) {
        String access = jwtTokenProvider.generateAccessToken(user.getId(), user.getKullaniciAdi());
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
