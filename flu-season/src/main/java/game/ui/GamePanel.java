package game.ui;

import game.core.*;
import game.core.RenderSnapshot.*;
import game.entity.UnitType;
import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel {
    private static final Color BG = new Color(40, 44, 52);
    private volatile RenderSnapshot snap = RenderSnapshot.empty();

    public GamePanel() { setPreferredSize(new Dimension(Constants.WIN_W, Constants.WIN_H)); }

    /** Goi tu game thread. repaint() an toan khi goi tu thread khac. */
    public void publish(RenderSnapshot s) { snap = s; repaint(); }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        RenderSnapshot s = snap;                       // doc 1 lan, dung suot lan ve

        g.setColor(BG);
        g.fillRect(0, 0, getWidth(), getHeight());
        drawTopBar(g, s);
        drawGrid(g);
        drawHover(g, s);
        drawDefenders(g, s);
        drawTokens(g, s);

        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        g.drawString("tick: " + s.tick(), 10, Constants.WIN_H - 8);
    }

    private void drawTopBar(Graphics2D g, RenderSnapshot s) {
        for (int i = 0; i < s.cards().size(); i++) {
            CardView c = s.cards().get(i);
            UnitType t = c.type();
            int x = Constants.CARD_X + i * Constants.CARD_STEP, y = Constants.CARD_Y;

            String atlas = (t == s.selected()) ? "unit_cards_selected.png" : "unit_cards.png";
            Assets.drawFrame(g, Assets.get(atlas), t.cardCol, t.cardRow, 144, 184,
                    x, y, Constants.CARD_W, Constants.CARD_H);

            if (!c.affordable() || c.cooldownLeft() > 0) {        // xam di
                g.setColor(new Color(0, 0, 0, 110));
                g.fillRect(x, y, Constants.CARD_W, Constants.CARD_H);
            }
            if (c.cooldownLeft() > 0) {                           // mask cooldown co lai dan
                int h = Constants.CARD_H * c.cooldownLeft() / t.cooldownTicks;
                g.setColor(new Color(0, 0, 0, 150));
                g.fillRect(x, y, Constants.CARD_W, h);
            }
        }

        // o credits
        g.setColor(new Color(20, 24, 30));
        g.fillRoundRect(Constants.CREDITS_X, Constants.CREDITS_Y, Constants.CREDITS_W, Constants.CREDITS_H, 12, 12);
        g.drawImage(Assets.get("credit.png"), Constants.CREDITS_X + 6, Constants.CREDITS_Y + 4, 32, 32, null);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        g.drawString(String.valueOf(s.credits()), Constants.CREDITS_X + 48, Constants.CREDITS_Y + 28);
    }

    private void drawGrid(Graphics2D g) {
        for (int r = 0; r < Constants.ROWS; r++)
            for (int c = 0; c < Constants.COLS; c++) {
                String f = ((r + c) % 2 == 0) ? "tile_classroom.png" : "tile_classroom_alt.png";
                g.drawImage(Assets.get(f), tileX(c), tileY(r), Constants.TILE, Constants.TILE, null);
            }
    }

    private void drawHover(Graphics2D g, RenderSnapshot s) {
        if (s.selected() == null || s.hoverRow() < 0) return;
        String f = s.hoverValid() ? "tile_placement_valid.png" : "tile_placement_invalid.png";
        g.drawImage(Assets.get(f), tileX(s.hoverCol()), tileY(s.hoverRow()), Constants.TILE, Constants.TILE, null);
    }

    private void drawDefenders(Graphics2D g, RenderSnapshot s) {
        for (DefenderView d : s.defenders()) {
            int x = tileX(d.col()) + (Constants.TILE - Constants.SPRITE_W) / 2;
            int y = tileY(d.row()) + (Constants.TILE - Constants.SPRITE_H) / 2;
            if (d.type() == UnitType.PC && d.pulseAge() < 12) {       // 12 tick, moi frame 3 tick
                Assets.drawFrame(g, Assets.get("animation_sheets/pc_credit_pulse.png"), d.pulseAge() / 3, 0,
                        160, 192, x, y, Constants.SPRITE_W, Constants.SPRITE_H);
            } else {
                g.drawImage(Assets.get("placed/" + d.type().sprite + ".png"),
                        x, y, Constants.SPRITE_W, Constants.SPRITE_H, null);
            }
        }
    }

    private void drawTokens(Graphics2D g, RenderSnapshot s) {
        for (TokenView t : s.tokens()) {
            if (t.ticksLeft() < 90 && (t.ticksLeft() / 6) % 2 == 0) continue;   // nhap nhay 3s cuoi
            g.drawImage(Assets.get("credit.png"), t.x(), t.y(), Constants.TOKEN_SIZE, Constants.TOKEN_SIZE, null);
        }
    }

    private static int tileX(int col) { return Constants.GRID_X + col * Constants.TILE; }
    private static int tileY(int row) { return Constants.GRID_Y + row * Constants.TILE; }
}
