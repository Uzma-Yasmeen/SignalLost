package com.signallost.server.store;

import com.signallost.server.verify.VerifiedRun;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One completed attempt, as the server worked it out.
 *
 * <p>Only {@code moves} came from the client. Every other number here was
 * produced by replaying those moves on this side, which is the whole reason the
 * table is worth trusting.
 */
@Entity
@Table(name = "run")
public class Run {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(name = "level_name", nullable = false, length = 40)
    private String levelName;

    @Column(name = "moves", nullable = false, length = 1000)
    private String moves;

    @Column(name = "stars", nullable = false)
    private int stars;

    @Column(name = "turns", nullable = false)
    private int turns;

    @Column(name = "par", nullable = false)
    private int par;

    @Column(name = "sightings", nullable = false)
    private int sightings;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    protected Run() {
        // for JPA
    }

    public Run(Player player, String moves, VerifiedRun verified) {
        this.player = player;
        this.moves = moves;
        this.levelName = verified.levelName();
        this.stars = verified.stars();
        this.turns = verified.turns();
        this.par = verified.par();
        this.sightings = verified.sightings();
        this.submittedAt = Instant.now();
    }

    public Long id() {
        return id;
    }

    public Player player() {
        return player;
    }

    public String levelName() {
        return levelName;
    }

    public String moves() {
        return moves;
    }

    public int stars() {
        return stars;
    }

    public int turns() {
        return turns;
    }

    public int par() {
        return par;
    }

    public int sightings() {
        return sightings;
    }

    public Instant submittedAt() {
        return submittedAt;
    }
}
