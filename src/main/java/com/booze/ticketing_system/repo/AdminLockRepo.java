package com.booze.ticketing_system.repo;

import com.booze.ticketing_system.entity.AdminLock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminLockRepo extends JpaRepository<AdminLock, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from AdminLock l where l.id = 1")
    Optional<AdminLock> lockSentinel();
}
