package com.example.gomokuexample;

/**
 * GomokuGame class – handles all game logic:
 * - board state (20x20)
 * - move validation
 * - win checking
 * - turn switching
 * - tracking moves and max row lengths
 * - resetting the game
 */
class GomokuGame {
    private int[][] board;
    private int currentPlayer;
    private boolean gameOver;
    private int winner;
    private final int boardSize;
    private int blackMoves;
    private int whiteMoves;

    public GomokuGame(int boardSize) {
        if (boardSize < 5) {
            throw new IllegalArgumentException("Board size must be at least 5.");
        }
        this.boardSize = boardSize;
        board = new int[boardSize][boardSize];
        currentPlayer = 1;
        gameOver = false;
        winner = 0;
        blackMoves = 0;
        whiteMoves = 0;
    }

    public GomokuGame() {
        this(20);
    }

    public boolean checkWin(int x, int y) {
        int[][][] directionLines = {
                {{0, 1}, {0, -1}},
                {{1, 0}, {-1, 0}},
                {{1, 1}, {-1, -1}},
                {{1, -1}, {-1, 1}}
        };
        for (int[][] oppositeDirs : directionLines) {
            int count = 1;
            for (int[] direction : oppositeDirs) {
                int dx = direction[0];
                int dy = direction[1];
                for (int i = 1; i < 5; i++) {
                    int newX = x + i * dx;
                    int newY = y + i * dy;
                    if (!isValidPosition(newX, newY) || board[newX][newY] != board[x][y]) {
                        break;
                    }
                    count++;
                    if (count >= 5) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean move(int x, int y) {
        if (gameOver) return false;
        if (!isValidPosition(x, y)) return false;
        if (board[x][y] != 0) return false;

        board[x][y] = currentPlayer;
        if (currentPlayer == 1) blackMoves++;
        else whiteMoves++;

        if (checkWin(x, y)) {
            gameOver = true;
            winner = currentPlayer;
        }
        if (!gameOver) {
            currentPlayer = (currentPlayer == 1) ? 2 : 1;
        }
        return true;
    }

    public void loseTurn() {
        if (!gameOver) {
            currentPlayer = (currentPlayer == 1) ? 2 : 1;
        }
    }

    public int getMaxUnbrokenRowLength(int player) {
        int max = 0;
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                if (board[i][j] == player) {
                    int[][] dirs = {{0,1}, {1,0}, {1,1}, {1,-1}};
                    for (int[] d : dirs) {
                        int len = 1;
                        int dx = d[0], dy = d[1];
                        for (int step = 1; step < 5; step++) {
                            int nx = i + step*dx, ny = j + step*dy;
                            if (!isValidPosition(nx, ny) || board[nx][ny] != player) break;
                            len++;
                        }
                        for (int step = 1; step < 5; step++) {
                            int nx = i - step*dx, ny = j - step*dy;
                            if (!isValidPosition(nx, ny) || board[nx][ny] != player) break;
                            len++;
                        }
                        if (len > max) max = len;
                    }
                }
            }
        }
        return max;
    }

    public void reset() {
        board = new int[boardSize][boardSize];
        currentPlayer = 1;
        gameOver = false;
        winner = 0;
        blackMoves = 0;
        whiteMoves = 0;
    }

    public int getBoardSize() { return boardSize; }
    public int[][] getBoard() { return board; }
    public int getCurrentPlayer() { return currentPlayer; }
    public boolean isGameOver() { return gameOver; }
    public int getWinner() { return winner; }
    public int getBlackMoves() { return blackMoves; }
    public int getWhiteMoves() { return whiteMoves; }

    public boolean isValidPosition(int x, int y) {
        return x >= 0 && x < boardSize && y >= 0 && y < boardSize;
    }

    public void render() { /* not used */ }
}