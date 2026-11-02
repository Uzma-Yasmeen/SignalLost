package com.signallost.server.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * What a client sends after finishing a level.
 *
 * <p>Note what is absent: there is no field for a score. The client reports
 * what it did, and the server works out what that was worth. A player who
 * wants three stars has to send a sequence of moves that earns three stars.
 *
 * @param playerName whose run it is
 * @param levelName  which level, as named in the catalogue
 * @param moves      one letter per turn: U, D, L, R or W
 */
public record SubmitRunRequest(
        @NotBlank(message = "a run needs a player name")
        @Size(max = 40, message = "a player name is at most 40 characters")
        String playerName,

        @NotBlank(message = "a run needs a level name")
        @Size(max = 40)
        @Pattern(regexp = "[a-zA-Z0-9_-]+", message = "a level name is letters, digits, _ and - only")
        String levelName,

        @NotBlank(message = "a run needs at least one move")
        @Size(max = 1000, message = "no level allows a run this long")
        @Pattern(regexp = "[UDLRWudlrw]+", message = "moves are the letters U, D, L, R and W")
        String moves) {
}
