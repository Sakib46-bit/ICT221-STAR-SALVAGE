package starsalvage.engine;

/**
 * The three-phase state cycle of a Turret enemy.
 *
 * Each time the player takes a turn the turret advances one phase:
 *   IDLE → CHARGING → FIRING → IDLE → …
 *
 * The turret only deals damage to the player when it is in the FIRING state
 * AND the player is in the same row or column (line of sight).
 */
public enum TurretState {
    IDLE,
    CHARGING,
    FIRING;

    /** Returns the next state in the cycle. */
    public TurretState next() {
        return switch (this) {
            case IDLE     -> CHARGING;
            case CHARGING -> FIRING;
            case FIRING   -> IDLE;
        };
    }

    @Override
    public String toString() {
        return switch (this) {
            case IDLE     -> "Idle";
            case CHARGING -> "Charging";
            case FIRING   -> "Firing";
        };
    }
}
