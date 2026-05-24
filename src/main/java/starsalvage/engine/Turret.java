package starsalvage.engine;

import java.io.Serializable;

/**
 * A stationary enemy turret.
 *
 * <p>State machine: IDLE → CHARGING → FIRING → IDLE → …
 * <p>Fires in line-of-sight (same row or column) only when FIRING.
 */
public class Turret implements Serializable {

    private int row;
    private int col;
    private TurretState state;
    private boolean alive;

    public Turret(int row, int col) {
        this.row   = row;
        this.col   = col;
        this.state = TurretState.IDLE;
        this.alive = true;
    }

    /** Advance the turret by one phase. */
    public void advanceState() {
        if (alive) {
            state = state.next();
        }
    }

    /**
     * Returns true if the turret is in FIRING state and
     * the player is in the same row or column (unobstructed check
     * is left to GameEngine).
     */
    public boolean canShootAt(int playerRow, int playerCol) {
        if (!alive || state != TurretState.FIRING) return false;
        return playerRow == row || playerCol == col;
    }

    // ── Getters / setters ──────────────────────────────────────────────────

    public int  getRow()   { return row; }
    public int  getCol()   { return col; }
    public TurretState getState() { return state; }
    public boolean isAlive()      { return alive; }

    public void setAlive(boolean alive) { this.alive = alive; }
    public void setState(TurretState state) { this.state = state; }

    @Override
    public String toString() {
        return String.format("Turret[%d,%d %s]", row, col, state);
    }
}
