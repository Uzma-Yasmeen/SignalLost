package com.signallost.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Phase 2: the leaderboard behind Signal Lost.
 *
 * <p>It does one thing the game cannot do for itself: decide whether a score is
 * real. The game runs on the player's machine, so anything it reports about its
 * own results is a claim rather than a fact. This service takes the moves
 * instead and replays them against the same rules, which turns the claim into
 * something it can check.
 *
 * <p>Run it with an in-memory database and no setup:
 *
 * <pre>
 *   mvn spring-boot:run
 * </pre>
 *
 * <p>Or against PostgreSQL, which is what a deployment would use:
 *
 * <pre>
 *   mvn spring-boot:run -Dspring-boot.run.profiles=postgres
 * </pre>
 */
@SpringBootApplication
public class ServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }
}
