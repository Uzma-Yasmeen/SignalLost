package com.signallost.server.api;

import com.signallost.level.LevelCatalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Refuses to come up without the game's levels on the classpath.
 *
 * <p>The server scores a run by replaying it against the real level, so without
 * the levels it cannot verify anything. Failing at startup is much easier to
 * diagnose than every submission being refused for a reason that sounds like
 * the player's fault.
 */
@Component
public class LevelsPresentCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LevelsPresentCheck.class);

    @Override
    public void run(ApplicationArguments args) {
        int levels = LevelCatalog.load().size();
        if (levels == 0) {
            throw new IllegalStateException(
                    "No levels found on the classpath. Install the game artifact first:"
                            + " mvn install -DskipTests in the repository root.");
        }
        log.info("Verifying runs against {} levels from the game's own catalogue", levels);
    }
}
