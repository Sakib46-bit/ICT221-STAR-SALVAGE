package starsalvage.engine;

/** The logical type of a map cell. */
public enum CellType {
    EMPTY,      // open space
    DEBRIS,     // space debris (may contain an item)
    ASTEROID,   // impassable obstacle
    EXIT,       // the escape pod / exit
    PLAYER,     // current player position (overlay)
    TURRET      // enemy turret (alive)
}
