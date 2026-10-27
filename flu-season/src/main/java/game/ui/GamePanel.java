package game.ui;

import game.core.*;
import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel {
    private volatile RenderSnapshot snap = new RenderSnapshot(GameState.PLAYING, 0, -1, -1);

    public GamePanel() { setPreferredSize(new Dimension(Constants.WIN_W, Constants.WIN_H)); }

    public void publish(RenderSnapshot s) { snap = s; repaint(); }  // repaint() gọi từ thread khác là an toàn

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        RenderSnapshot s = snap;                       // đọc 1 lần
        g.setColor(new Color(40, 44, 52)); g.fillRect(0, 0, getWidth(), getHeight());
        for (int r = 0; r < Constants.ROWS; r++)
            for (int c = 0; c < Constants.COLS; c++) {
                g.setColor((r + c) % 2 == 0 ? new Color(200, 215, 190) : new Color(180, 200, 170));
                g.fillRect(Constants.GRID_X + c * Constants.TILE, Constants.GRID_Y + r * Constants.TILE,
                        Constants.TILE, Constants.TILE);
            }
        if (s.hoverRow() >= 0) {
            g.setColor(new Color(255, 255, 0, 90));
            g.fillRect(Constants.GRID_X + s.hoverCol() * Constants.TILE,
                    Constants.GRID_Y + s.hoverRow() * Constants.TILE, Constants.TILE, Constants.TILE);
        }
        g.setColor(Color.WHITE); g.drawString("tick: " + s.tick(), 10, 20);
    }
}