package com.signallost.server.store;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RunRepository extends JpaRepository<Run, Long> {

    /**
     * The leaderboard for one level: each player's best run, best first.
     *
     * <p>Every attempt is stored, so a player who has finished a level forty
     * times has forty rows. A leaderboard wants one of them, which is a
     * "greatest per group" query — the window function numbers each player's
     * runs from their best and the outer query keeps the first.
     *
     * <p>Doing it in SQL rather than sorting in Java matters once the table is
     * not small: the database can answer it from the ordered index, and the
     * amount of data crossing the wire is the size of the leaderboard rather
     * than the size of the history.
     *
     * <p>Written as native SQL because window functions are not JPQL. Both
     * PostgreSQL and H2 run this unchanged, which is why the tests exercise the
     * same statement production does.
     */
    @Query(value = """
            SELECT * FROM (
                SELECT r.*,
                       ROW_NUMBER() OVER (
                           PARTITION BY r.player_id
                           ORDER BY r.stars DESC, r.turns ASC, r.id ASC
                       ) AS rank_for_player
                  FROM run r
                 WHERE r.level_name = :levelName
            ) best
            WHERE best.rank_for_player = 1
            ORDER BY best.stars DESC, best.turns ASC, best.submitted_at ASC
            """, nativeQuery = true)
    List<Run> bestPerPlayerOn(@Param("levelName") String levelName, Limit limit);

    List<Run> findByPlayerDisplayNameOrderBySubmittedAtDesc(String displayName, Limit limit);

    long countByLevelName(String levelName);
}
