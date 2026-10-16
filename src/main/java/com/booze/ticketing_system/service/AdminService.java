package com.booze.ticketing_system.service;

import com.booze.ticketing_system.dto.AuthResponse;
import com.booze.ticketing_system.dto.LoginRequest;
import com.booze.ticketing_system.dto.RegisterRequest;
import com.booze.ticketing_system.entity.Admin;
import com.booze.ticketing_system.entity.AdminPrincipal;
import com.booze.ticketing_system.exception.EmailAlreadyUsedException;
import com.booze.ticketing_system.exception.UserLimitExceededException;
import com.booze.ticketing_system.repo.AdminLockRepo;
import com.booze.ticketing_system.repo.AdminRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminService {
    private static final int MAX_ADMINS = 3;

    private final AdminRepo adminRepo;
    private final AdminLockRepo adminLockRepo;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse signup(RegisterRequest request) {
        adminLockRepo.lockSentinel()
                .orElseThrow(() -> new IllegalStateException(
                        "Admin lock row missing — did the seeder run?"));

        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (adminRepo.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyUsedException();
        }

        if (adminRepo.count() >= MAX_ADMINS) {
            throw new UserLimitExceededException(
                    "Cannot create more than " + MAX_ADMINS + " admins"
            );
        }

        Admin user = Admin.builder()
                .name(request.name().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .build();

        try {
            adminRepo.saveAndFlush(user);
        } catch (DataIntegrityViolationException raceLost) {
            throw new EmailAlreadyUsedException();
        }

        return issueTokens(user.getId());
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (BadCredentialsException e) {
            throw e;
        }

        AdminPrincipal principal = (AdminPrincipal) authentication.getPrincipal();

        return issueTokens(principal.getId());
    }

    private AuthResponse issueTokens(String userId) {
        return new AuthResponse(
                jwtService.generateAccessToken(userId),
                refreshTokenService.issueNewFamily(userId),
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds());
    }
}
