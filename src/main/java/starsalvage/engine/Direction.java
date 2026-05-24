package starsalvage.engine;

/** The four cardinal directions a player or enemy can move. */
public enum Direction {
    NORTH, SOUTH, EAST, WEST;

    /** Returns the row-delta for this direction. */
    public int dRow() {
        return switch (this) {
            case NORTH -> -1;
            case SOUTH ->  1;
            default    ->  0;
        };
    }

    /** Returns the col-delta for this direction. */
    public int dCol() {
        return switch (this) {
            case WEST -> -1;
            case EAST ->  1;
            default   ->  0;
        };
    }
}
