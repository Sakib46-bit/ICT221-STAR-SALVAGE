package starsalvage.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The player's spaceship navigating the salvage field.
 */
public class Player implements Serializable {

    public static final int MAX_HEALTH  = 10;
    public static final int MAX_SHIELDS = 5;

    private int row;
    private int col;
    private int health;
    private int shields;
    private int fuel;
    private int credits;
    private int weapons;
    private final List<Item> inventory;

    public Player(int startRow, int startCol) {
        this.row       = startRow;
        this.col       = startCol;
        this.health    = MAX_HEALTH;
        this.shields   = 0;
        this.fuel      = 0;
        this.credits   = 0;
        this.weapons   = 0;
        this.inventory = new ArrayList<>();
    }

    // ── Movement ──────────────────────────────────────────────────────────

    public void moveTo(int newRow, int newCol) {
        this.row = newRow;
        this.col = newCol;
    }

    // ── Combat ────────────────────────────────────────────────────────────

    /**
     * Take damage. Shields absorb first; remaining hits health.
     *
     * @param amount damage amount
     */
    public void takeDamage(int amount) {
        if (shields > 0) {
            int absorbed = Math.min(shields, amount);
            shields -= absorbed;
            amount  -= absorbed;
        }
        health = Math.max(0, health - amount);
    }

    /** @return true if the player has no health remaining */
    public boolean isDead() { return health <= 0; }

    // ── Item collection ───────────────────────────────────────────────────

    public void collectItem(Item item) {
        inventory.add(item);
        switch (item) {
            case FUEL    -> fuel++;
            case SHIELD  -> shields = Math.min(shields + 2, MAX_SHIELDS);
            case WEAPON  -> weapons++;
            case CREDITS -> credits += 10;
        }
    }

    /** Use a weapon charge to destroy a turret (returns false if no weapons). */
    public boolean useWeapon() {
        if (weapons <= 0) return false;
        weapons--;
        return true;
    }

    // ── Serialisation snapshot (for undo) ─────────────────────────────────

    public PlayerSnapshot snapshot() {
        return new PlayerSnapshot(row, col, health, shields, fuel, credits, weapons,
                new ArrayList<>(inventory));
    }

    public void restore(PlayerSnapshot snap) {
        this.row      = snap.row();
        this.col      = snap.col();
        this.health   = snap.health();
        this.shields  = snap.shields();
        this.fuel     = snap.fuel();
        this.credits  = snap.credits();
        this.weapons  = snap.weapons();
        this.inventory.clear();
        this.inventory.addAll(snap.inventory());
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public int getRow()      { return row; }
    public int getCol()      { return col; }
    public int getHealth()   { return health; }
    public int getShields()  { return shields; }
    public int getFuel()     { return fuel; }
    public int getCredits()  { return credits; }
    public int getWeapons()  { return weapons; }
    public List<Item> getInventory() { return Collections.unmodifiableList(inventory); }

    // ── Setters (engine-internal) ─────────────────────────────────────────

    public void setHealth(int h)  { this.health  = Math.max(0, Math.min(MAX_HEALTH, h)); }
    public void setShields(int s) { this.shields = Math.max(0, Math.min(MAX_SHIELDS, s)); }
    public void setFuel(int f)    { this.fuel    = Math.max(0, f); }

    @Override
    public String toString() {
        return String.format("Player[%d,%d HP=%d SH=%d FU=%d CR=%d WP=%d]",
                row, col, health, shields, fuel, credits, weapons);
    }

    // ── Inner snapshot record ─────────────────────────────────────────────

    public record PlayerSnapshot(int row, int col, int health, int shields,
                                  int fuel, int credits, int weapons,
                                  List<Item> inventory) implements Serializable {}
}
