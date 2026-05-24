package starsalvage.engine;

import java.io.*;
import java.util.*;

/**
 * Core game engine for Star Salvage.
 *
 * Rules:
 *  - Player starts at (0,0) and must reach EXIT at (size-1, size-1).
 *  - Asteroids are impassable.
 *  - Debris cells contain items (Fuel/Shield/Weapon/Credits).
 *  - Turrets cycle IDLE -> CHARGING -> FIRING each turn.
 *  - When FIRING, turrets damage player in same row or column.
 *  - Player can shoot adjacent turrets using Weapon items.
 *  - Undo: up to 10 levels using full state snapshots.
 *  - Save/Load: full serialisation to file.
 *  - Wait: skip movement, enemies still advance.
 */
public class GameEngine implements Serializable {

    public static final int TURRET_DAMAGE   = 2;
    public static final int MAX_UNDO_LEVELS = 10;
    public static final int DEFAULT_SIZE    = 10;

    private Cell[][]     map;
    private Player       player;
    private List<Turret> turrets;
    private int          size;
    private int          turn;
    private boolean      gameOver;
    private boolean      playerWon;
    private String       lastMessage;
    private final Deque<GameState> undoStack = new ArrayDeque<>();

    public GameEngine(int size) {
        this.size = size;
        initGame(size, new Random());
    }

    public GameEngine(int size, long seed) {
        this.size = size;
        initGame(size, new Random(seed));
    }

    private void initGame(int size, Random rng) {
        this.turn        = 0;
        this.gameOver    = false;
        this.playerWon   = false;
        this.lastMessage = "Welcome to Star Salvage! Reach the EXIT (E).";
        this.turrets     = new ArrayList<>();

        map = new Cell[size][size];
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                map[r][c] = new Cell(CellType.EMPTY);

        map[size-1][size-1].setType(CellType.EXIT);

        // Asteroids ~15%
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if ((r == 0 && c == 0) || (r == size-1 && c == size-1)) continue;
                if (rng.nextDouble() < 0.15) map[r][c].setType(CellType.ASTEROID);
            }
        }

        // Debris with items ~20% of empty
        Item[] items = Item.values();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (map[r][c].getType() != CellType.EMPTY) continue;
                if (r == 0 && c == 0) continue;
                if (rng.nextDouble() < 0.20) {
                    map[r][c] = new Cell(CellType.DEBRIS, items[rng.nextInt(items.length)]);
                }
            }
        }

        // Turrets (2-4)
        int numTurrets = 2 + rng.nextInt(3);
        int placed = 0, attempts = 0;
        while (placed < numTurrets && attempts < 500) {
            attempts++;
            int r = rng.nextInt(size);
            int c = rng.nextInt(size);
            if ((r == 0 && c == 0) || (r == size-1 && c == size-1)) continue;
            if (map[r][c].getType() == CellType.ASTEROID) continue;
            if (isTurretAt(r, c)) continue;
            if (r <= 1 && c <= 1) continue;
            map[r][c].setType(CellType.TURRET);
            turrets.add(new Turret(r, c));
            placed++;
        }

        player = new Player(0, 0);
        ensurePathExists(rng);
    }

    private void ensurePathExists(Random rng) {
        if (!pathExists()) {
            for (int c = 0; c < size; c++) {
                if (map[0][c].getType() == CellType.ASTEROID) map[0][c].setType(CellType.EMPTY);
                removeTurretAt(0, c);
            }
            for (int r = 0; r < size; r++) {
                if (map[r][size-1].getType() == CellType.ASTEROID) map[r][size-1].setType(CellType.EMPTY);
                removeTurretAt(r, size-1);
            }
        }
    }

    private boolean pathExists() {
        boolean[][] visited = new boolean[size][size];
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{0, 0});
        visited[0][0] = true;
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int r = cur[0], c = cur[1];
            if (r == size-1 && c == size-1) return true;
            for (int[] d : dirs) {
                int nr = r+d[0], nc = c+d[1];
                if (!inBounds(nr,nc) || visited[nr][nc]) continue;
                CellType t = map[nr][nc].getType();
                if (t == CellType.ASTEROID) continue;
                visited[nr][nc] = true;
                queue.add(new int[]{nr, nc});
            }
        }
        return false;
    }

    // ── Actions ──────────────────────────────────────────────────────────

    public boolean movePlayer(Direction dir) {
        if (gameOver) { lastMessage = "Game is over."; return false; }
        int nr = player.getRow() + dir.dRow();
        int nc = player.getCol() + dir.dCol();

        if (!inBounds(nr, nc)) {
            lastMessage = "Can't move that way – out of bounds!"; return false;
        }
        CellType target = map[nr][nc].getType();
        if (target == CellType.ASTEROID) {
            lastMessage = "Blocked by an asteroid!"; return false;
        }
        if (target == CellType.TURRET) {
            lastMessage = "A turret blocks the way! Use shoot <dir> to destroy it."; return false;
        }

        pushUndoState();
        player.moveTo(nr, nc);

        if (target == CellType.DEBRIS && map[nr][nc].hasItem()) {
            Item collected = map[nr][nc].getItem();
            player.collectItem(collected);
            map[nr][nc].setItem(null);
            map[nr][nc].setType(CellType.EMPTY);
            lastMessage = "Collected: " + collected.getDisplayName() + "! " + playerStatus();
        } else if (target == CellType.EXIT) {
            lastMessage = "You reached the EXIT! You win!";
            gameOver = true; playerWon = true;
        } else {
            lastMessage = "Moved " + dir + ". " + playerStatus();
        }

        advanceEnemies();
        turn++;
        return true;
    }

    public void waitTurn() {
        if (gameOver) { lastMessage = "Game is over."; return; }
        pushUndoState();
        lastMessage = "Waiting... " + playerStatus();
        advanceEnemies();
        turn++;
    }

    public boolean shootTurret(Direction dir) {
        if (gameOver) { lastMessage = "Game is over."; return false; }
        int tr = player.getRow() + dir.dRow();
        int tc = player.getCol() + dir.dCol();
        if (!inBounds(tr, tc) || map[tr][tc].getType() != CellType.TURRET) {
            lastMessage = "No turret in that direction."; return false;
        }
        if (!player.useWeapon()) {
            lastMessage = "No weapons! Collect Weapon items first."; return false;
        }
        pushUndoState();
        removeTurretAt(tr, tc);
        map[tr][tc].setType(CellType.EMPTY);
        lastMessage = "Turret destroyed! Weapons left: " + player.getWeapons();
        advanceEnemies();
        turn++;
        return true;
    }

    public boolean undo() {
        if (undoStack.isEmpty()) { lastMessage = "Nothing to undo."; return false; }
        restoreState(undoStack.pop());
        lastMessage = "Undo successful. " + playerStatus();
        return true;
    }

    // ── Save / Load ───────────────────────────────────────────────────────

    public void saveGame(String filePath) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(this);
        }
    }

    public static GameEngine loadGame(String filePath) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            return (GameEngine) ois.readObject();
        }
    }

    // ── Enemy logic ───────────────────────────────────────────────────────

    private void advanceEnemies() {
        if (gameOver) return;
        for (Turret t : turrets) {
            if (!t.isAlive()) continue;
            t.advanceState();
            if (t.canShootAt(player.getRow(), player.getCol())) {
                player.takeDamage(TURRET_DAMAGE);
                lastMessage += String.format(" ⚡ Turret[%d,%d] FIRED! HP:%d",
                        t.getRow(), t.getCol(), player.getHealth());
            }
        }
        if (player.isDead()) {
            gameOver = true; playerWon = false;
            lastMessage += " ☠ You have been destroyed!";
        }
    }

    // ── Undo helpers ─────────────────────────────────────────────────────

    private void pushUndoState() {
        if (undoStack.size() >= MAX_UNDO_LEVELS) {
            // remove oldest (tail of ArrayDeque when used as stack)
            Iterator<GameState> it = undoStack.descendingIterator();
            GameState oldest = null;
            while (it.hasNext()) oldest = it.next();
            undoStack.remove(oldest);
        }
        undoStack.push(snapshot());
    }

    private GameState snapshot() {
        Cell[][] mapCopy = new Cell[size][size];
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                mapCopy[r][c] = new Cell(map[r][c]);

        List<GameState.TurretSnapshot> tSnaps = new ArrayList<>();
        for (Turret t : turrets)
            tSnaps.add(new GameState.TurretSnapshot(t.getRow(), t.getCol(), t.getState(), t.isAlive()));

        return new GameState(mapCopy, player.snapshot(), tSnaps, turn, gameOver, playerWon, lastMessage);
    }

    private void restoreState(GameState s) {
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                map[r][c] = new Cell(s.map()[r][c]);

        player.restore(s.player());

        turrets.clear();
        for (GameState.TurretSnapshot ts : s.turrets()) {
            Turret t = new Turret(ts.row(), ts.col());
            t.setState(ts.state());
            t.setAlive(ts.alive());
            turrets.add(t);
        }

        turn = s.turn(); gameOver = s.gameOver();
        playerWon = s.playerWon(); lastMessage = s.lastMessage();
    }

    // ── Utility ───────────────────────────────────────────────────────────

    public boolean inBounds(int r, int c) { return r>=0 && r<size && c>=0 && c<size; }

    private boolean isTurretAt(int r, int c) {
        for (Turret t : turrets) if (t.isAlive() && t.getRow()==r && t.getCol()==c) return true;
        return false;
    }

    private void removeTurretAt(int r, int c) {
        for (Turret t : turrets) if (t.getRow()==r && t.getCol()==c) { t.setAlive(false); return; }
    }

    private String playerStatus() {
        return String.format("HP:%d SH:%d FU:%d CR:%d WP:%d",
                player.getHealth(), player.getShields(),
                player.getFuel(), player.getCredits(), player.getWeapons());
    }

    public String renderMap() {
        StringBuilder sb = new StringBuilder();
        sb.append("   ");
        for (int c = 0; c < size; c++) sb.append(String.format("%2d", c));
        sb.append("\n");
        for (int r = 0; r < size; r++) {
            sb.append(String.format("%2d ", r));
            for (int c = 0; c < size; c++) {
                if (r == player.getRow() && c == player.getCol()) sb.append(" @");
                else sb.append(" ").append(map[r][c].symbol());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        GameEngine engine = new GameEngine(DEFAULT_SIZE, 42L);
        System.out.println("=== Star Salvage (CLI) ===");
        System.out.println("Commands: n s e w  wait  shoot <dir>  undo  save  load  help  quit");
        while (!engine.isGameOver()) {
            System.out.println("\nTurn " + engine.getTurn());
            System.out.println(engine.renderMap());
            System.out.println(engine.getLastMessage());
            System.out.print("> ");
            String[] parts = sc.nextLine().trim().toLowerCase().split("\\s+");
            switch (parts[0]) {
                case "n","north" -> engine.movePlayer(Direction.NORTH);
                case "s","south" -> engine.movePlayer(Direction.SOUTH);
                case "e","east"  -> engine.movePlayer(Direction.EAST);
                case "w","west"  -> engine.movePlayer(Direction.WEST);
                case "wait"      -> engine.waitTurn();
                case "undo"      -> engine.undo();
                case "shoot" -> {
                    if (parts.length < 2) { System.out.println("shoot <n|s|e|w>"); break; }
                    Direction d = switch(parts[1]) {
                        case "n","north" -> Direction.NORTH; case "s","south" -> Direction.SOUTH;
                        case "e","east"  -> Direction.EAST;  case "w","west"  -> Direction.WEST;
                        default -> null; };
                    if (d != null) engine.shootTurret(d); else System.out.println("Unknown dir.");
                }
                case "save" -> { try { engine.saveGame("starsalvage.sav"); System.out.println("Saved."); }
                                 catch (IOException ex) { System.out.println("Save failed: "+ex.getMessage()); } }
                case "load" -> { try { engine = GameEngine.loadGame("starsalvage.sav"); System.out.println("Loaded."); }
                                 catch (Exception ex) { System.out.println("Load failed: "+ex.getMessage()); } }
                case "quit","q" -> { System.out.println("Goodbye!"); return; }
                default -> System.out.println("Unknown command. Try: n s e w wait shoot undo save load quit");
            }
        }
        System.out.println(engine.renderMap());
        System.out.println(engine.getLastMessage());
        System.out.println(engine.isPlayerWon() ? "VICTORY!" : "GAME OVER");
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public int          getSize()        { return size; }
    public Cell[][]     getMap()         { return map; }
    public Player       getPlayer()      { return player; }
    public List<Turret> getTurrets()     { return Collections.unmodifiableList(turrets); }
    public int          getTurn()        { return turn; }
    public boolean      isGameOver()     { return gameOver; }
    public boolean      isPlayerWon()    { return playerWon; }
    public String       getLastMessage() { return lastMessage; }
    public int          getUndoCount()   { return undoStack.size(); }
}
