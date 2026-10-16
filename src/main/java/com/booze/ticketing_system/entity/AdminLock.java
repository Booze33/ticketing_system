package com.booze.ticketing_system.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "admin_lock")
@Getter
@Setter
public class AdminLock {
    @Id
    private Long id;
}
