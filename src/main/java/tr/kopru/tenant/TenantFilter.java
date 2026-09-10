package tr.kopru.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import tr.kopru.config.JwtTokenProvider;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final OrgAccessService orgAccessService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = extractBearerToken(request);
            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                UUID userId = jwtTokenProvider.parseUserId(token);
                TenantContext.setUserId(userId);

                String orgHeader = request.getHeader("X-Org-Id");
                List<SimpleGrantedAuthority> authorities = Collections.emptyList();

                if (StringUtils.hasText(orgHeader)) {
                    try {
                        UUID orgId = UUID.fromString(orgHeader);
                        Optional<OrgAccessService.OrgAuth> authOpt =
                                orgAccessService.resolveActiveMembership(userId, orgId);

                        if (authOpt.isEmpty()) {
                            // User is not an active member of the requested org -> 403 Forbidden
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\":{\"code\":\"FORBIDDEN\",\"message\":\"Kullanici bu organizasyonun aktif uyesi degildir.\"}}");
                            return;
                        }

                        OrgAccessService.OrgAuth orgAuth = authOpt.get();
                        TenantContext.setOrgId(orgId);
                        authorities = List.of(
                                new SimpleGrantedAuthority("ROLE_" + orgAuth.rol().name()),
                                new SimpleGrantedAuthority("ORG_TYPE_" + orgAuth.orgTip().name())
                        );
                    } catch (IllegalArgumentException e) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"error\":{\"code\":\"VALIDATION_ERROR\",\"message\":\"Gecersiz X-Org-Id formati.\"}}");
                        return;
                    }
                }

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
