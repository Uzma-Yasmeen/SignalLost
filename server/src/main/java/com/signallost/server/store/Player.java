package com.signallost.server.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Somebody who has submitted at least one run, identified only by a name. */
@Entity
@Table(name = "player")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_name", nullable = false, unique = true, length = 40)
    private String displayName;

    @Column(name = "first_seen", nullable = false)
    private Instant firstSeen;

    protected Player() {
        // for JPA
    }

    public Player(String displayName) {
        this.displayName = displayName;
        this.firstSeen = Instant.now();
    }

    public Long id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public Instant firstSeen() {
        return firstSeen;
    }
}
