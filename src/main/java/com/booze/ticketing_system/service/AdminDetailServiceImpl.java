package com.booze.ticketing_system.service;

import com.booze.ticketing_system.entity.Admin;
import com.booze.ticketing_system.entity.AdminPrincipal;
import com.booze.ticketing_system.repo.AdminRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminDetailServiceImpl implements UserDetailsService {
    private final AdminRepo adminRepo;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Admin user = adminRepo.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
        return AdminPrincipal.from(user);
    }

    public AdminPrincipal loadById(String userId) {
        Admin user = adminRepo.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return AdminPrincipal.forToken(user);
    }
}
