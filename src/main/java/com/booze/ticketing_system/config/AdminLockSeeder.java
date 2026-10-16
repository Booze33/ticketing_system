package com.booze.ticketing_system.config;

import com.booze.ticketing_system.entity.AdminLock;
import com.booze.ticketing_system.repo.AdminLockRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminLockSeeder implements CommandLineRunner {
    private final AdminLockRepo adminLockRepo;

    @Override
    public void run(String... args) {
        if (adminLockRepo.findById(1L).isEmpty()) {
            AdminLock lock = new AdminLock();
            lock.setId(1L);
            adminLockRepo.save(lock);
        }
    }
}
