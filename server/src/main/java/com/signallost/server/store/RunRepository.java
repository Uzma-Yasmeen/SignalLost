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

    /**
     * One player's best star count on each level they have finished.
     *
     * <p>This is the player's saved progress, and it is a query rather than a
     * table for the same reason "best run" is: the runs are the record, and
     * anything derived from them stays correct by being derived every time.
     */
    @Query(value = """
            SELECT r.level_name AS levelName, MAX(r.stars) AS stars
              FROM run r
              JOIN player p ON p.id = r.player_id
             WHERE LOWER(p.display_name) = LOWER(:playerName)
             GROUP BY r.level_name
            """, nativeQuery = true)
    List<LevelStars> bestStarsPerLevel(@Param("playerName") String playerName);

    /**
     * Everybody, ranked by the stars they hold across all levels.
     *
     * <p>A player's total is the sum of their best on each level, so the inner
     * query finds the bests and the outer one adds them up. Ties go to whoever
     * has finished more levels, then to the earlier name, so the order is
     * stable from one request to the next.
     */
    @Query(value = """
            SELECT best.display_name AS playerName,
                   SUM(best.stars) AS stars,
                   COUNT(*) AS levelsFinished
              FROM (
                    SELECT p.display_name, r.level_name, MAX(r.stars) AS stars
                      FROM run r
                      JOIN player p ON p.id = r.player_id
                     GROUP BY p.display_name, r.level_name
                   ) best
             GROUP BY best.display_name
             ORDER BY SUM(best.stars) DESC, COUNT(*) DESC, best.display_name ASC
            """, nativeQuery = true)
    List<Standing> standings(Limit limit);

    /** A level and the most stars one player has earned on it. */
    interface LevelStars {
        String getLevelName();
        int getStars();
    }

    /** One row of the overall leaderboard. */
    interface Standing {
        String getPlayerName();
        long getStars();
        long getLevelsFinished();
    }

    List<Run> findByPlayerDisplayNameOrderBySubmittedAtDesc(String displayName, Limit limit);

    long countByLevelName(String levelName);
}
