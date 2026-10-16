package com.booze.ticketing_system.config;

import com.booze.ticketing_system.entity.AdminPrincipal;
import com.booze.ticketing_system.service.AdminDetailServiceImpl;
import com.booze.ticketing_system.service.JwtService;
import com.booze.ticketing_system.service.TokenRevocationService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AdminDetailServiceImpl userDetailsService;
    private final TokenRevocationService revocationService;

    private void authenticate(String token) {
        try {
            Claims claims = jwtService.parseToken(token);
            String jti = claims.getId();
            String userId = claims.getSubject();

            if (jti == null || userId == null || revocationService.isRevoked(jti)) {
                return;
            }

            AdminPrincipal user = userDetailsService.loadById(userId);
            if (!user.isEnabled()) {
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            SecurityContextHolder.clearContext();
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            authenticate(header.substring(PREFIX.length()).trim());
        }

        filterChain.doFilter(request, response);
    }
}
