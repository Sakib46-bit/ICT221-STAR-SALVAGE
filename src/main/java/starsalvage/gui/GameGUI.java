package starsalvage.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX Application entry point for Star Salvage.
 *
 * NOTE: Run RunGame.main() in IntelliJ, not this class directly.
 */
public class GameGUI extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("game_gui.fxml"));
        Scene scene = new Scene(root, 900, 700);
        // Dark background for the whole window
        scene.setFill(javafx.scene.paint.Color.web("#0a0a1a"));
        primaryStage.setScene(scene);
        primaryStage.setTitle("★ Star Salvage");
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    /** In IntelliJ, do NOT run this method. Run RunGame.main() instead. */
    public static void main(String[] args) {
        launch(args);
    }
}
