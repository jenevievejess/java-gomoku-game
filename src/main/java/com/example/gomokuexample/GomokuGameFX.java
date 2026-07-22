package com.example.gomokuexample;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * GomokuGameFX – Traditional Gomoku game
 * Features: smaller board, turn bowls at top, pause function, stats on right,
 * temporary board messages, game‑over overlay (blur + buttons).
 */
public class GomokuGameFX extends Application {
    // Board constants
    private static final int BOARD_SIZE = 20;
    private static final int CELL_SIZE = 30;
    private static final int TOLERANCE = 15;
    private static final int BOARD_OFFSET = CELL_SIZE / 2;
    private static final int TIME_LIMIT_SEC = 30;

    private GomokuGame game;
    private Canvas canvas;
    private GraphicsContext gc;
    private StackPane canvasContainer;
    private Label tempMessage;

    // UI components
    private Label timerLabel;
    private Label turnLabel;
    private Label blackMovesLabel;
    private Label whiteMovesLabel;
    private Label blackMaxRowLabel;
    private Label whiteMaxRowLabel;
    private Circle blackBowl;
    private Circle whiteBowl;
    private BorderPane root;
    private Button pauseButton;

    // Timer and pause
    private Timeline timer;
    private int timeLeft;
    private boolean paused = false;
    private Label pauseOverlay;

    // Game over overlay
    private StackPane gameOverOverlay;

    @Override
    public void start(Stage primaryStage) {
        game = new GomokuGame(BOARD_SIZE);

        root = new BorderPane();
        root.setStyle("-fx-background-color: #d2b48c;");

        // ----- TOP PANEL -----
        VBox topCenter = new VBox(5);
        topCenter.setAlignment(Pos.CENTER);
        turnLabel = new Label("Black's Turn");
        turnLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        turnLabel.setTextFill(Color.BLACK);
        timerLabel = new Label("⏱️ " + TIME_LIMIT_SEC + "s");
        timerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        timerLabel.setTextFill(Color.BLACK);
        topCenter.getChildren().addAll(turnLabel, timerLabel);

        VBox blackBox = createBowlBox(Color.BLACK, "BLACK");
        blackBowl = (Circle) blackBox.getChildren().get(0);
        VBox whiteBox = createBowlBox(Color.WHITE, "WHITE");
        whiteBowl = (Circle) whiteBox.getChildren().get(0);

        HBox topPanel = new HBox(40);
        topPanel.setAlignment(Pos.CENTER);
        topPanel.setPadding(new Insets(20, 20, 10, 20));
        topPanel.getChildren().addAll(blackBox, topCenter, whiteBox);
        root.setTop(topPanel);

        // ----- CENTER: canvas with pause overlay and temporary message -----
        canvas = new Canvas(CELL_SIZE * BOARD_SIZE, CELL_SIZE * BOARD_SIZE);
        gc = canvas.getGraphicsContext2D();
        drawBoard();

        canvasContainer = new StackPane();
        canvasContainer.getChildren().add(canvas);
        canvasContainer.setPadding(new Insets(10));
        root.setCenter(canvasContainer);

        // Temporary message label (initially invisible)
        tempMessage = new Label();
        tempMessage.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        tempMessage.setTextFill(Color.WHITE);
        tempMessage.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 10 20; -fx-background-radius: 10;");
        tempMessage.setVisible(false);
        canvasContainer.getChildren().add(tempMessage);
        StackPane.setAlignment(tempMessage, Pos.CENTER);

        // ----- RIGHT PANEL: stats and vertical buttons -----
        VBox rightPanel = new VBox(15);
        rightPanel.setPadding(new Insets(20));
        rightPanel.setStyle("-fx-background-color: #f5deb3; -fx-border-color: #8b5a2b;");
        rightPanel.setPrefWidth(180);

        Label statsTitle = new Label("Game Stats");
        statsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        blackMovesLabel = new Label("Black moves: 0");
        whiteMovesLabel = new Label("White moves: 0");
        blackMaxRowLabel = new Label("Black max row: 0");
        whiteMaxRowLabel = new Label("White max row: 0");

        Separator sep = new Separator();

        Button resetBtn = new Button("New Game");
        resetBtn.setStyle("-fx-background-color: #8b5a2b; -fx-text-fill: white; -fx-cursor: hand;");
        resetBtn.setMaxWidth(Double.MAX_VALUE);
        resetBtn.setOnAction(e -> resetGame());

        pauseButton = new Button("Pause");
        pauseButton.setStyle("-fx-background-color: #8b5a2b; -fx-text-fill: white; -fx-cursor: hand;");
        pauseButton.setMaxWidth(Double.MAX_VALUE);
        pauseButton.setOnAction(e -> togglePause());

        Button exitBtn = new Button("Exit");
        exitBtn.setStyle("-fx-background-color: #8b5a2b; -fx-text-fill: white; -fx-cursor: hand;");
        exitBtn.setMaxWidth(Double.MAX_VALUE);
        exitBtn.setOnAction(e -> Platform.exit());

        rightPanel.getChildren().addAll(statsTitle, blackMovesLabel, whiteMovesLabel,
                blackMaxRowLabel, whiteMaxRowLabel, sep, resetBtn, pauseButton, exitBtn);
        root.setRight(rightPanel);

        canvas.setOnMouseClicked(e -> handleMouseClick(e.getX(), e.getY()));

        Scene scene = new Scene(root, 900, 720);
        primaryStage.setTitle("Gomoku Game");
        primaryStage.setScene(scene);
        primaryStage.show();

        startTimer();
        updateStats();
        updateBowlHighlight();
    }

    private VBox createBowlBox(Color bowlColor, String labelText) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        Circle bowl = new Circle(35);
        bowl.setFill(bowlColor);
        bowl.setStroke(Color.GRAY);
        bowl.setStrokeWidth(2);
        Label lbl = new Label(labelText);
        lbl.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        lbl.setTextFill(Color.BLACK);
        box.getChildren().addAll(bowl, lbl);
        return box;
    }

    private void drawBoard() {
        gc.setFill(Color.rgb(205, 155, 105));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.5);
        for (int i = 0; i < BOARD_SIZE; i++) {
            double pos = i * CELL_SIZE + BOARD_OFFSET;
            gc.strokeLine(BOARD_OFFSET, pos, BOARD_SIZE * CELL_SIZE - BOARD_OFFSET, pos);
            gc.strokeLine(pos, BOARD_OFFSET, pos, BOARD_SIZE * CELL_SIZE - BOARD_OFFSET);
        }

        int[][] starPoints = {{3,3},{3,16},{16,3},{16,16},{9,9},{9,10},{10,9},{10,10}};
        for (int[] p : starPoints) {
            if (p[0] < BOARD_SIZE && p[1] < BOARD_SIZE) {
                double x = p[1] * CELL_SIZE + BOARD_OFFSET;
                double y = p[0] * CELL_SIZE + BOARD_OFFSET;
                gc.setFill(Color.BLACK);
                gc.fillOval(x-4, y-4, 8, 8);
            }
        }

        int[][] board = game.getBoard();
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                int stone = board[row][col];
                if (stone != 0) {
                    double x = col * CELL_SIZE + BOARD_OFFSET;
                    double y = row * CELL_SIZE + BOARD_OFFSET;
                    double radius = CELL_SIZE * 0.4;
                    if (stone == 1) {
                        gc.setFill(Color.BLACK);
                        gc.fillOval(x-radius, y-radius, radius*2, radius*2);
                        gc.setStroke(Color.GRAY);
                        gc.strokeOval(x-radius, y-radius, radius*2, radius*2);
                    } else {
                        gc.setFill(Color.WHITE);
                        gc.fillOval(x-radius, y-radius, radius*2, radius*2);
                        gc.setStroke(Color.BLACK);
                        gc.strokeOval(x-radius, y-radius, radius*2, radius*2);
                    }
                }
            }
        }
    }

    private void handleMouseClick(double mouseX, double mouseY) {
        if (game.isGameOver() || paused) return;
        int[] pos = snapToGrid(mouseX, mouseY);
        if (pos == null) return;
        int row = pos[0], col = pos[1];
        if (game.getBoard()[row][col] != 0) {
            showTemporaryMessage("❌ Stone already here!");
            return;
        }
        if (game.move(row, col)) {
            drawBoard();
            updateStats();
            resetTimer();
            if (game.isGameOver()) {
                stopTimer();
                showGameOverOverlay(game.getWinner() == 1 ? "BLACK" : "WHITE");
            }
        }
    }

    private void showTemporaryMessage(String msg) {
        tempMessage.setText(msg);
        tempMessage.setVisible(true);
        Timeline hideTimer = new Timeline(new KeyFrame(Duration.seconds(1.5), e -> tempMessage.setVisible(false)));
        hideTimer.play();
    }

    /**
     * Game Over overlay: blurs the whole root, shows winner and two buttons.
     */
    private void showGameOverOverlay(String winner) {
        // Blur the entire root (including all panels)
        root.setEffect(new GaussianBlur(10));

        // Create overlay StackPane that covers the whole root
        gameOverOverlay = new StackPane();
        gameOverOverlay.setStyle("-fx-background-color: rgba(0,0,0,0.7);");
        gameOverOverlay.setAlignment(Pos.CENTER);

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: #d2b48c; -fx-background-radius: 15; -fx-border-color: #8b5a2b; -fx-border-width: 3;");

        Label winLabel = new Label("🏆 " + winner + " WINS! 🏆");
        winLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        winLabel.setTextFill(winner.equals("BLACK") ? Color.BLACK : Color.WHITE);
        winLabel.setStyle("-fx-effect: dropshadow(gaussian, gold, 10, 0, 0, 0);");

        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);

        Button newGameBtn = new Button("New Game");
        newGameBtn.setStyle("-fx-background-color: #8b5a2b; -fx-text-fill: white; -fx-font-size: 16px; -fx-cursor: hand;");
        newGameBtn.setOnAction(e -> {
            // Remove overlay and blur, then reset game
            root.setEffect(null);
            ((Pane) root.getParent()).getChildren().remove(gameOverOverlay);
            resetGame();
        });

        Button exitBtn = new Button("Exit");
        exitBtn.setStyle("-fx-background-color: #8b5a2b; -fx-text-fill: white; -fx-font-size: 16px; -fx-cursor: hand;");
        exitBtn.setOnAction(e -> Platform.exit());

        buttonBox.getChildren().addAll(newGameBtn, exitBtn);
        content.getChildren().addAll(winLabel, buttonBox);
        gameOverOverlay.getChildren().add(content);

        // Add overlay to the scene's root (the BorderPane is inside a Pane, but we can add to the scene's root directly)
        // Since root is the main container, we need to wrap it or add overlay as sibling. Simpler: get the scene's root group.
        Pane sceneRoot = (Pane) root.getParent();
        if (sceneRoot == null) {
            // If root is directly the scene's root, we need to wrap it. But here root is added to scene.
            // We'll temporarily reparent root into a new StackPane.
            Scene scene = root.getScene();
            StackPane newRoot = new StackPane();
            newRoot.getChildren().add(root);
            scene.setRoot(newRoot);
            sceneRoot = newRoot;
        }
        sceneRoot.getChildren().add(gameOverOverlay);
        StackPane.setAlignment(gameOverOverlay, Pos.CENTER);
    }

    private void togglePause() {
        paused = !paused;
        if (paused) {
            pauseButton.setText("Resume");
            canvas.setEffect(new GaussianBlur(10));
            pauseOverlay = new Label("⏸");
            pauseOverlay.setFont(Font.font(80));
            pauseOverlay.setTextFill(Color.WHITE);
            pauseOverlay.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-padding: 20;");
            pauseOverlay.setAlignment(Pos.CENTER);
            canvasContainer.getChildren().add(pauseOverlay);
            StackPane.setAlignment(pauseOverlay, Pos.CENTER);
            stopTimer();
        } else {
            pauseButton.setText("Pause");
            canvas.setEffect(null);
            canvasContainer.getChildren().remove(pauseOverlay);
            if (!game.isGameOver()) startTimer();
        }
    }

    private int[] snapToGrid(double mx, double my) {
        int bestRow = -1, bestCol = -1;
        double bestDist = Double.MAX_VALUE;
        for (int r=0; r<BOARD_SIZE; r++) {
            for (int c=0; c<BOARD_SIZE; c++) {
                double cx = c*CELL_SIZE + BOARD_OFFSET;
                double cy = r*CELL_SIZE + BOARD_OFFSET;
                double d = Math.hypot(mx-cx, my-cy);
                if (d < bestDist) {
                    bestDist = d;
                    bestRow = r;
                    bestCol = c;
                }
            }
        }
        return bestDist <= TOLERANCE ? new int[]{bestRow, bestCol} : null;
    }

    private void updateStats() {
        blackMovesLabel.setText("Black moves: " + game.getBlackMoves());
        whiteMovesLabel.setText("White moves: " + game.getWhiteMoves());
        blackMaxRowLabel.setText("Black max row: " + game.getMaxUnbrokenRowLength(1));
        whiteMaxRowLabel.setText("White max row: " + game.getMaxUnbrokenRowLength(2));
        updateTurnDisplay();
    }

    private void updateTurnDisplay() {
        if (!game.isGameOver()) {
            int cur = game.getCurrentPlayer();
            turnLabel.setText(cur == 1 ? "Black's Turn" : "White's Turn");
            updateBowlHighlight();
        } else {
            turnLabel.setText("Game Over");
        }
    }

    private void updateBowlHighlight() {
        if (game.isGameOver()) return;
        int cur = game.getCurrentPlayer();
        if (cur == 1) {
            blackBowl.setEffect(new DropShadow(20, Color.BLUE));
            whiteBowl.setEffect(null);
        } else {
            whiteBowl.setEffect(new DropShadow(20, Color.BLUE));
            blackBowl.setEffect(null);
        }
    }

    private void startTimer() {
        stopTimer();
        timeLeft = TIME_LIMIT_SEC;
        timerLabel.setText("⏱️ " + timeLeft + "s");
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (!game.isGameOver() && !paused) {
                timeLeft--;
                timerLabel.setText("⏱️ " + timeLeft + "s");
                if (timeLeft <= 0) {
                    stopTimer();
                    game.loseTurn();
                    drawBoard();
                    updateStats();
                    resetTimer();
                }
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private void stopTimer() {
        if (timer != null) timer.stop();
    }

    private void resetTimer() {
        stopTimer();
        startTimer();
    }

    private void resetGame() {
        stopTimer();
        game.reset();
        if (paused) togglePause();
        drawBoard();
        updateStats();
        startTimer();
        updateTurnDisplay();
        tempMessage.setVisible(false);
        // If game over overlay is visible, remove it and blur
        if (gameOverOverlay != null && root.getScene() != null) {
            root.setEffect(null);
            Pane parent = (Pane) root.getParent();
            if (parent != null) parent.getChildren().remove(gameOverOverlay);
            gameOverOverlay = null;
        }
    }

    public static void main(String[] args) { launch(args); }
}