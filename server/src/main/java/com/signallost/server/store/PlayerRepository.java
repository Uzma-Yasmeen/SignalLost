package com.signallost.server.store;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    Optional<Player> findByDisplayNameIgnoreCase(String displayName);
}
