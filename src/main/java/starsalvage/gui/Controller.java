package starsalvage.gui;

import starsalvage.engine.*;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

/**
 * JavaFX Controller for Star Salvage.
 *
 * Handles all GUI interactions and delegates game logic to GameEngine.
 */
public class Controller {

    // ── FXML bindings ─────────────────────────────────────────────────────

    @FXML private GridPane gridPane;
    @FXML private Label    lblHp, lblShields, lblFuel, lblCredits, lblWeapons, lblTurn;
    @FXML private TextArea msgLog;
    @FXML private Label    statusBar;

    // ── Engine ────────────────────────────────────────────────────────────

    private GameEngine engine;

    private static final int CELL_SIZE = 52;

    // ── Initialisation ────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        engine = new GameEngine(GameEngine.DEFAULT_SIZE);
        updateGui();
        log("New game started. Reach the EXIT (E)!");

        // Keyboard controls (requires scene focus)
        Platform.runLater(() -> {
            if (gridPane.getScene() != null) {
                gridPane.getScene().addEventFilter(KeyEvent.KEY_PRESSED, this::handleKey);
                gridPane.getScene().getRoot().setFocusTraversable(true);
                gridPane.getScene().getRoot().requestFocus();
            }
        });
    }

    // ── Keyboard handler ──────────────────────────────────────────────────

    private void handleKey(KeyEvent event) {
        switch (event.getCode()) {
            case W, UP    -> { engine.movePlayer(Direction.NORTH); refresh(); event.consume(); }
            case S, DOWN  -> { engine.movePlayer(Direction.SOUTH); refresh(); event.consume(); }
            case A, LEFT  -> { engine.movePlayer(Direction.WEST);  refresh(); event.consume(); }
            case D, RIGHT -> { engine.movePlayer(Direction.EAST);  refresh(); event.consume(); }
            case SPACE    -> { engine.waitTurn();                  refresh(); event.consume(); }
            case Z        -> { engine.undo();                      refresh(); event.consume(); }
            default -> {}
        }
    }

    // ── Button handlers ───────────────────────────────────────────────────

    @FXML void onNorth() { engine.movePlayer(Direction.NORTH); refresh(); }
    @FXML void onSouth() { engine.movePlayer(Direction.SOUTH); refresh(); }
    @FXML void onEast()  { engine.movePlayer(Direction.EAST);  refresh(); }
    @FXML void onWest()  { engine.movePlayer(Direction.WEST);  refresh(); }
    @FXML void onWait()  { engine.waitTurn();                  refresh(); }
    @FXML void onUndo()  { engine.undo();                      refresh(); }

    @FXML void onShootNorth() { engine.shootTurret(Direction.NORTH); refresh(); }
    @FXML void onShootSouth() { engine.shootTurret(Direction.SOUTH); refresh(); }
    @FXML void onShootEast()  { engine.shootTurret(Direction.EAST);  refresh(); }
    @FXML void onShootWest()  { engine.shootTurret(Direction.WEST);  refresh(); }

    @FXML
    void onNewGame() {
        engine = new GameEngine(GameEngine.DEFAULT_SIZE);
        msgLog.clear();
        updateGui();
        log("New game started! Navigate to EXIT (E).");
    }

    @FXML
    void onSave() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Game");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Save files", "*.sav"));
        fc.setInitialFileName("starsalvage.sav");
        File f = fc.showSaveDialog(getStage());
        if (f == null) return;
        try {
            engine.saveGame(f.getAbsolutePath());
            statusBar.setText("Game saved to: " + f.getName());
            log("Game saved.");
        } catch (IOException ex) {
            showAlert("Save Error", ex.getMessage());
        }
    }

    @FXML
    void onLoad() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Load Game");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Save files", "*.sav"));
        File f = fc.showOpenDialog(getStage());
        if (f == null) return;
        try {
            engine = GameEngine.loadGame(f.getAbsolutePath());
            msgLog.clear();
            updateGui();
            log("Game loaded from: " + f.getName());
            statusBar.setText("Loaded: " + f.getName());
        } catch (Exception ex) {
            showAlert("Load Error", ex.getMessage());
        }
    }

    // ── GUI update ────────────────────────────────────────────────────────

    /** Called after every action to refresh the grid and status panels. */
    private void refresh() {
        updateGui();
        log(engine.getLastMessage());
        if (engine.isGameOver()) {
            String result = engine.isPlayerWon()
                    ? "🚀 VICTORY! You escaped the debris field!"
                    : "💀 SHIP DESTROYED! Game over.";
            showAlert(engine.isPlayerWon() ? "You Win!" : "Game Over", result);
        }
    }

    private void updateGui() {
        renderGrid();
        updateStatusPanel();
    }

    /** Renders the map grid. */
    private void renderGrid() {
        gridPane.getChildren().clear();
        gridPane.setGridLinesVisible(false);

        Cell[][]  map    = engine.getMap();
        Player    player = engine.getPlayer();
        int       size   = engine.getSize();

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                StackPane cell = buildCell(map[r][c],
                        r == player.getRow() && c == player.getCol(), r, c);
                gridPane.add(cell, c, r);
            }
        }
    }

    /** Builds a single visual cell. */
    private StackPane buildCell(Cell cell, boolean hasPlayer, int row, int col) {
        StackPane pane = new StackPane();
        pane.setPrefSize(CELL_SIZE, CELL_SIZE);
        pane.setMaxSize(CELL_SIZE, CELL_SIZE);

        // Background colour
        String bgColor = switch (cell.getType()) {
            case ASTEROID -> "#222233";
            case EXIT     -> "#003300";
            case DEBRIS   -> "#1a1a00";
            case TURRET   -> "#220000";
            default       -> "#050510";
        };
        pane.setStyle("-fx-background-color: " + bgColor + "; -fx-border-color: #1a1a2a; -fx-border-width: 0.5px;");

        // Content text / symbol
        String symbol;
        String color;

        if (hasPlayer) {
            symbol = "✦";   color = "#00ffff";
        } else {
            symbol = switch (cell.getType()) {
                case EMPTY    -> "·";
                case ASTEROID -> "▓";
                case EXIT     -> "E";
                case TURRET   -> turretSymbol(row, col);
                case DEBRIS   -> cell.hasItem() ? cell.getItem().getSymbol() : "·";
                case PLAYER   -> "@";
            };
            color = switch (cell.getType()) {
                case EMPTY    -> "#1a1a3a";
                case ASTEROID -> "#555566";
                case EXIT     -> "#00ff44";
                case TURRET   -> turretColor(row, col);
                case DEBRIS   -> itemColor(cell.getItem());
                case PLAYER   -> "#00ffff";
            };
        }

        Text text = new Text(symbol);
        text.setFill(Color.web(color));
        text.setFont(Font.font("monospace", 18));
        pane.getChildren().add(text);

        return pane;
    }

    private String turretSymbol(int row, int col) {
        for (var t : engine.getTurrets()) {
            if (t.isAlive() && t.getRow() == row && t.getCol() == col) {
                return switch (t.getState()) {
                    case IDLE     -> "○";
                    case CHARGING -> "◉";
                    case FIRING   -> "⊛";
                };
            }
        }
        return "T";
    }

    private String turretColor(int row, int col) {
        for (var t : engine.getTurrets()) {
            if (t.isAlive() && t.getRow() == row && t.getCol() == col) {
                return switch (t.getState()) {
                    case IDLE     -> "#994422";
                    case CHARGING -> "#ff8800";
                    case FIRING   -> "#ff2200";
                };
            }
        }
        return "#ff4422";
    }

    private String itemColor(Item item) {
        if (item == null) return "#ffffff";
        return switch (item) {
            case FUEL    -> "#ffaa33";
            case SHIELD  -> "#4488ff";
            case WEAPON  -> "#ff6644";
            case CREDITS -> "#aaffaa";
        };
    }

    /** Updates the right-hand status panel. */
    private void updateStatusPanel() {
        Player p = engine.getPlayer();
        lblHp.setText(      String.format("HP:  %d / %d", p.getHealth(), Player.MAX_HEALTH));
        lblShields.setText( String.format("SH:  %d", p.getShields()));
        lblFuel.setText(    String.format("FU:  %d", p.getFuel()));
        lblCredits.setText( String.format("CR:  %d", p.getCredits()));
        lblWeapons.setText( String.format("WP:  %d", p.getWeapons()));
        lblTurn.setText(    String.format("Turn: %d  [Undo: %d]", engine.getTurn(), engine.getUndoCount()));

        // Colour HP label based on health
        double hpRatio = (double) p.getHealth() / Player.MAX_HEALTH;
        String hpColor = hpRatio > 0.6 ? "#66ff66" : hpRatio > 0.3 ? "#ffaa33" : "#ff3333";
        lblHp.setStyle("-fx-text-fill: " + hpColor + "; -fx-font-family: monospace;");

        statusBar.setText(String.format("Pos: (%d,%d)  Turrets alive: %d",
                p.getRow(), p.getCol(),
                (int) engine.getTurrets().stream().filter(t -> t.isAlive()).count()));
    }

    /** Appends a line to the message log. */
    private void log(String msg) {
        if (msg == null || msg.isEmpty()) return;
        msgLog.appendText(msg + "\n");
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Stage getStage() {
        return (Stage) gridPane.getScene().getWindow();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
