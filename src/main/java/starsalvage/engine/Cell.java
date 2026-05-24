package starsalvage.engine;

import java.io.Serializable;

/**
 * A single cell on the game map.
 *
 * <p>The engine uses plain data cells; the GUI builds its own visual
 * representations on top of the engine state.
 */
public class Cell implements Serializable {

    private CellType type;
    private Item     item;      // non-null only when type == DEBRIS (before collection)

    public Cell(CellType type) {
        this.type = type;
        this.item = null;
    }

    public Cell(CellType type, Item item) {
        this.type = type;
        this.item = item;
    }

    // Copy constructor (for snapshots)
    public Cell(Cell other) {
        this.type = other.type;
        this.item = other.item;
    }

    public CellType getType()               { return type; }
    public void     setType(CellType type)  { this.type = type; }

    public Item  getItem()           { return item; }
    public void  setItem(Item item)  { this.item = item; }

    /** @return true when this cell holds an uncollected debris item */
    public boolean hasItem() { return item != null; }

    /** CLI symbol for this cell (used in text rendering). */
    public String symbol() {
        return switch (type) {
            case EMPTY    -> ".";
            case DEBRIS   -> (item != null ? item.getSymbol() : "d");
            case ASTEROID -> "#";
            case EXIT     -> "E";
            case PLAYER   -> "@";
            case TURRET   -> "T";
        };
    }

    @Override
    public String toString() { return symbol(); }
}
