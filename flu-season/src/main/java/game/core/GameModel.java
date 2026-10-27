package game.core;

public class GameModel {
    private GameState state = GameState.PLAYING;
    private long tick = 0;
    private int hoverRow = -1, hoverCol = -1;

    public void setHover(int r, int c) { hoverRow = r; hoverCol = c; }
    public void setState(GameState s) { state = s; }

    public void update() {
        if (state != GameState.PLAYING) return;
        tick++;
        // Bước sau: spawn, PC, defender, projectile, enemy, va chạm, thắng/thua
    }

    public RenderSnapshot snapshot() {
        return new RenderSnapshot(state, tick, hoverRow, hoverCol);
    }
}