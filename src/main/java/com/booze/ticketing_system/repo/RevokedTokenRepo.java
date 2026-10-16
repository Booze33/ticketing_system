package com.booze.ticketing_system.repo;

import com.booze.ticketing_system.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RevokedTokenRepo extends JpaRepository<RevokedToken, Long> {
    boolean existsByJti(String jti);

    @Modifying
    @Query("delete from RevokedToken t where t.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
