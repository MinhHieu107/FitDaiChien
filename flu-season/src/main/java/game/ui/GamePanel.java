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
        drawGrid(g, s);
        drawHover(g, s);
        drawDefenders(g, s);
        drawEnemies(g, s);
        drawProjectiles(g, s);
        drawTokens(g, s);
        drawBottomBar(g, s);
        drawOverlay(g, s);
    }

    // ---------------- Top bar ----------------

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

        g.setColor(new Color(20, 24, 30));
        g.fillRoundRect(Constants.CREDITS_X, Constants.CREDITS_Y, Constants.CREDITS_W, Constants.CREDITS_H, 12, 12);
        g.drawImage(Assets.get("credit.png"), Constants.CREDITS_X + 6, Constants.CREDITS_Y + 4, 32, 32, null);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        g.drawString(String.valueOf(s.credits()), Constants.CREDITS_X + 48, Constants.CREDITS_Y + 28);
    }

    // ---------------- Luoi ----------------

    private void drawGrid(Graphics2D g, RenderSnapshot s) {
        for (int r = 0; r < Constants.ROWS; r++)
            for (int c = 0; c < Constants.COLS; c++) {
                String f = ((r + c) % 2 == 0) ? "tile_classroom.png" : "tile_classroom_alt.png";
                g.drawImage(Assets.get(f), tileX(c), tileY(r), Constants.TILE, Constants.TILE, null);
                if (!s.activeRows().get(r)) {                    // hang khong dung duoc: toi di
                    g.setColor(new Color(30, 30, 34, 210));
                    g.fillRect(tileX(c), tileY(r), Constants.TILE, Constants.TILE);
                }
            }
    }

    private void drawHover(Graphics2D g, RenderSnapshot s) {
        if (s.selected() == null || s.hoverRow() < 0) return;
        String f = s.hoverValid() ? "tile_placement_valid.png" : "tile_placement_invalid.png";
        g.drawImage(Assets.get(f), tileX(s.hoverCol()), tileY(s.hoverRow()), Constants.TILE, Constants.TILE, null);
    }

    // ---------------- Thuc the ----------------

    private void drawDefenders(Graphics2D g, RenderSnapshot s) {
        for (DefenderView d : s.defenders()) {
            int x = tileX(d.col()) + (Constants.TILE - Constants.SPRITE_W) / 2;
            int y = tileY(d.row()) + (Constants.TILE - Constants.SPRITE_H) / 2;
            UnitType t = d.type();
            int w = Constants.SPRITE_W, h = Constants.SPRITE_H;

            if (t == UnitType.PC && d.actionAge() < 12) {                  // 12 tick, moi frame 3 tick
                Assets.drawFrame(g, Assets.get("animation_sheets/pc_credit_pulse.png"),
                        d.actionAge() / 3, 0, 160, 192, x, y, w, h);
            } else if (isStudent(t) && d.actionAge() < 12) {               // dang ban: wind-up -> return
                Assets.drawFrame(g, Assets.get("student_throw_actions_atlas.png"),
                        d.actionAge() / 3, yearRow(t), 160, 192, x, y, w, h);
            } else {
                g.drawImage(Assets.get("placed/" + t.sprite + ".png"), x, y, w, h, null);
            }
            if (d.health() < t.health) drawHealthBar(g, x + 12, y - 2, 40, d.health() / (double) t.health);
        }
    }

    private void drawEnemies(Graphics2D g, RenderSnapshot s) {
        for (EnemyView e : s.enemies()) {
            // hitbox [x, x+0.6] -> can giua sprite vao giua hitbox
            int cx = (int) (Constants.GRID_X + (e.x() + 0.3) * Constants.TILE);
            int x = cx - Constants.SPRITE_W / 2;
            int y = tileY(e.row()) + (Constants.TILE - Constants.SPRITE_H) / 2;

            if (e.biting()) {   // dang can: dung bite atlas, hang theo loai ke dich
                Assets.drawFrame(g, Assets.get("animation_sheets/enemy_bite_atlas.png"),
                        (e.age() / 4) % 4, e.type().biteRow, 160, 192,
                        x, y, Constants.SPRITE_W, Constants.SPRITE_H);
            } else {
                Assets.drawFrame(g, Assets.get(e.type().walkSheet),
                        (e.age() / 6) % 4, 0, 160, 192,
                        x, y, Constants.SPRITE_W, Constants.SPRITE_H);
            }
            if (e.health() < e.type().health)
                drawHealthBar(g, x + 12, y - 2, 40, e.health() / (double) e.type().health);
        }
    }

    private void drawProjectiles(Graphics2D g, RenderSnapshot s) {
        for (ProjectileView p : s.projectiles()) {
            int size = 28;
            int cx = (int) (Constants.GRID_X + (p.px() + 0.1) * Constants.TILE);
            int cy = tileY(p.row()) + 42;                // do cao tay nhan vat
            g.drawImage(Assets.get(p.kind().sprite), cx - size / 2, cy - size / 2, size, size, null);
        }
    }

    private void drawTokens(Graphics2D g, RenderSnapshot s) {
        for (TokenView t : s.tokens()) {
            if (t.ticksLeft() < 90 && (t.ticksLeft() / 6) % 2 == 0) continue;   // nhap nhay 3s cuoi
            g.drawImage(Assets.get("credit.png"), t.x(), t.y(), Constants.TOKEN_SIZE, Constants.TOKEN_SIZE, null);
        }
    }

    private void drawHealthBar(Graphics2D g, int x, int y, int w, double frac) {
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(x - 1, y - 1, w + 2, 6);
        g.setColor(frac > 0.5 ? new Color(90, 200, 90) : frac > 0.25 ? new Color(230, 190, 50) : new Color(220, 70, 60));
        g.fillRect(x, y, (int) Math.round(w * Math.max(0, frac)), 4);
    }

    // ---------------- Bottom bar & overlay ----------------

    private void drawBottomBar(Graphics2D g, RenderSnapshot s) {
        g.setColor(new Color(20, 24, 30));
        g.fillRoundRect(90, 510, 720, 44, 12, 12);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        g.drawString(s.message(), 104, 538);

        g.setColor(new Color(20, 24, 30));                         // thanh tien do wave
        g.fillRect(90, 570, 720, 8);
        g.setColor(new Color(150, 100, 220));
        g.fillRect(90, 570, (int) (720 * s.waveProgress()), 8);
    }

    private void drawOverlay(Graphics2D g, RenderSnapshot s) {
        String title, sub;
        switch (s.state()) {
            case PAUSED -> { title = "PAUSED"; sub = "Press P to resume"; }
            case WON    -> { title = "Stage cleared!"; sub = s.message() + "   (click to restart)"; }
            case LOST   -> { title = "Marked absent"; sub = s.message() + "   (click to retry)"; }
            default -> { return; }
        }
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, Constants.WIN_W, Constants.WIN_H);
        g.setColor(Color.WHITE);
        drawCentered(g, title, 270, new Font(Font.SANS_SERIF, Font.BOLD, 44));
        drawCentered(g, sub, 320, new Font(Font.SANS_SERIF, Font.PLAIN, 18));
    }

    private void drawCentered(Graphics2D g, String text, int baselineY, Font font) {
        g.setFont(font);
        int w = g.getFontMetrics().stringWidth(text);
        g.drawString(text, (Constants.WIN_W - w) / 2, baselineY);
    }

    // ---------------- Tien ich ----------------

    private static boolean isStudent(UnitType t) {
        return t == UnitType.YEAR1 || t == UnitType.YEAR2 || t == UnitType.YEAR3 || t == UnitType.YEAR4;
    }
    private static int yearRow(UnitType t) {
        return switch (t) { case YEAR1 -> 0; case YEAR2 -> 1; case YEAR3 -> 2; default -> 3; };
    }

    private static int tileX(int col) { return Constants.GRID_X + col * Constants.TILE; }
    private static int tileY(int row) { return Constants.GRID_Y + row * Constants.TILE; }
}
