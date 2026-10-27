package game.ui;

import game.core.*;
import java.awt.event.*;

public class InputHandler extends MouseAdapter {
    private final GameLoop loop;
    public InputHandler(GameLoop loop) { this.loop = loop; }

    private static int[] toTile(MouseEvent e) {
        int c = (e.getX() - Constants.GRID_X) / Constants.TILE;
        int r = (e.getY() - Constants.GRID_Y) / Constants.TILE;
        boolean in = e.getX() >= Constants.GRID_X && e.getY() >= Constants.GRID_Y
                && r >= 0 && r < Constants.ROWS && c >= 0 && c < Constants.COLS;
        return in ? new int[]{r, c} : new int[]{-1, -1};
    }

    @Override public void mouseMoved(MouseEvent e) {
        int[] t = toTile(e);
        loop.submit(m -> m.setHover(t[0], t[1]));   // lambda = Command
    }
}