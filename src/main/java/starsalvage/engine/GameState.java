package starsalvage.engine;

import java.io.Serializable;
import java.util.List;

/**
 * An immutable snapshot of the full game state used by the undo stack.
 */
public record GameState(
        Cell[][]            map,
        Player.PlayerSnapshot player,
        List<TurretSnapshot> turrets,
        int                 turn,
        boolean             gameOver,
        boolean             playerWon,
        String              lastMessage
) implements Serializable {

    /** Lightweight turret snapshot. */
    public record TurretSnapshot(int row, int col, TurretState state, boolean alive)
            implements Serializable {}
}
