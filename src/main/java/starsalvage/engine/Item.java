package starsalvage.engine;

/** Items that can be collected from space debris cells. */
public enum Item {
    FUEL("Fuel", "F"),
    SHIELD("Shield", "S"),
    WEAPON("Weapon", "W"),
    CREDITS("Credits", "C");

    private final String displayName;
    private final String symbol;

    Item(String displayName, String symbol) {
        this.displayName = displayName;
        this.symbol = symbol;
    }

    public String getDisplayName() { return displayName; }
    public String getSymbol()      { return symbol; }

    @Override
    public String toString() { return displayName; }
}
