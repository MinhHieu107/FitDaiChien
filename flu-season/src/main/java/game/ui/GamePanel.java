package game.ui;

import game.core.*;
import game.core.RenderSnapshot.*;
import game.entity.BossAction;
import game.entity.UnitType;
import game.stage.Stages;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class GamePanel extends JPanel {
    private static final Color BG = new Color(40, 44, 52);
    private enum Tint { NONE, WHITE, GREY }

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

        switch (s.state()) {
            case MENU -> drawMenu(g, s);
            case STAGE_SELECT -> drawStageSelect(g, s);
            default -> {                                  // PLAYING, PAUSED, WON, LOST, ENDING
                drawTopBar(g, s);
                drawGrid(g, s);
                drawHover(g, s);
                drawDefenders(g, s);
                drawEnemies(g, s);
                drawEffects(g, s);
                drawProjectiles(g, s);
                drawTokens(g, s);
                drawFloats(g, s);
                drawBottomBar(g, s);
                drawBanner(g, s);
                drawOverlay(g, s);
            }
        }
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

        drawButton(g, s, Layout.PAUSE_BTN, "Pause", s.state() == GameState.PLAYING);
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

    // ---------------- Defender ----------------

    private void drawDefenders(Graphics2D g, RenderSnapshot s) {
        for (DefenderView d : s.defenders()) {
            int x = tileX(d.col()) + (Constants.TILE - Constants.SPRITE_W) / 2;
            int y = tileY(d.row()) + (Constants.TILE - Constants.SPRITE_H) / 2;
            UnitType t = d.type();
            Tint tint = d.stunned() ? Tint.GREY : Tint.NONE;
            int w = Constants.SPRITE_W, h = Constants.SPRITE_H;

            if (t == UnitType.PC && d.actionAge() < 12) {                  // 12 tick, moi frame 3 tick
                drawFrame(g, Assets.get("animation_sheets/pc_credit_pulse.png"), d.actionAge() / 3, 0, 160, 192, x, y, w, h, tint);
            } else if (isStudent(t) && d.actionAge() < 12) {               // dang ban: wind-up -> return
                drawFrame(g, Assets.get("student_throw_actions_atlas.png"), d.actionAge() / 3, yearRow(t), 160, 192, x, y, w, h, tint);
            } else if (t == UnitType.CHARGER) {                            // hang = trang thai MINE / BOMB / UNSTABLE
                drawFrame(g, Assets.get("animation_sheets/charger_state_animation.png"),
                        d.visualFrame(), d.visualRow(), 160, 192, x, y, w, h, tint);
            } else if (t == UnitType.WEABOO && d.visualRow() >= 0) {       // dang dung chieu
                drawFrame(g, Assets.get("weaboo_moves_atlas.png"), d.visualFrame(), d.visualRow(), 160, 192, x, y, w, h, tint);
            } else if (t == UnitType.GAMER && d.health() * 2 < t.health) {  // duoi 50% mau: sprite "tired"
                drawFrame(g, Assets.get("gamer_tired.png"), 0, 0, 160, 192, x, y, w, h, tint);
            } else {
                drawFrame(g, Assets.get("placed/" + t.sprite + ".png"), 0, 0, 160, 192, x, y, w, h, tint);
            }

            if (d.stunned()) {                                             // dau "?" nhap nhom tren dau
                int bob = (int) (Math.sin(s.tick() / 4.0) * 3);
                g.drawImage(Assets.get("prof_question_mark.png"), x + w / 2 - 16, y - 24 + bob, 32, 32, null);
            }
            if (d.health() < t.health) drawHealthBar(g, x + 12, y - 2, 40, d.health() / (double) t.health);
        }
    }

    // ---------------- Ke dich & boss ----------------

    private void drawEnemies(Graphics2D g, RenderSnapshot s) {
        Composite old = g.getComposite();
        for (EnemyView e : s.enemies()) {
            if (e.boss()) { drawBoss(g, e); continue; }
            int cx = (int) (Constants.GRID_X + (e.x() + 0.3) * Constants.TILE);     // hitbox [x, x+0.6]
            int x = cx - Constants.SPRITE_W / 2;
            int y = tileY(e.row()) + (Constants.TILE - Constants.SPRITE_H) / 2;
            Tint tint = e.flash() ? Tint.WHITE : Tint.NONE;

            if (e.alpha() < 1.0) g.setComposite(AlphaComposite.SrcOver.derive((float) e.alpha()));
            if (e.biting()) {
                drawFrame(g, Assets.get("animation_sheets/enemy_bite_atlas.png"), (e.age() / 4) % 4, e.type().biteRow,
                        160, 192, x, y, Constants.SPRITE_W, Constants.SPRITE_H, tint);
            } else {
                drawFrame(g, Assets.get(e.type().walkSheet), (e.age() / 6) % 4, 0,
                        160, 192, x, y, Constants.SPRITE_W, Constants.SPRITE_H, tint);
            }
            g.setComposite(old);
            if (e.alpha() >= 1.0 && e.health() < e.type().health)
                drawHealthBar(g, x + 12, y - 2, 40, e.health() / (double) e.type().health);
        }
    }

    /** Boss ve to hon (96x112), hitbox [x, x+1.0]. Chon anh theo hanh dong -> phase. */
    private void drawBoss(Graphics2D g, EnemyView e) {
        final int w = 96, h = 112;
        int cx = (int) (Constants.GRID_X + (e.x() + 0.5) * Constants.TILE);
        int x = cx - w / 2;
        int y = tileY(e.row()) + 76 - h;                       // chan boss ngang chan cac nhan vat khac
        Tint tint = e.flash() ? Tint.WHITE : Tint.NONE;
        BossAction a = e.action();

        if (a != null) {
            int frame = a == BossAction.DEFEAT ? Math.min(3, e.actionAge() / 22)     // giu pose cuoi
                    : a.ticks > 12 ? (e.actionAge() / 6) % 4                      // pose dai: lap lai
                    : Math.min(3, e.actionAge() / 3);                             // pose ngan: 12 tick
            drawFrame(g, Assets.get(a.sprite), frame, 0, 160, 192, x, y, w, h, tint);
        } else if (e.phase() == 2) {                                                // Finals Rage: aura do lap lai
            drawFrame(g, Assets.get("prof_finals_rage_strip.png"), (e.age() / 6) % 4, 0, 160, 192, x, y, w, h, tint);
        } else if (e.phase() == 1) {                                                // Pop Quiz: o phase 2, nhun nhe
            drawFrame(g, Assets.get("prof_deadline_phase_sprites.png"), 1, 0, 160, 192, x, y + ((e.age() / 6) % 2) * 2, w, h, tint);
        } else {                                                                    // Lecture: di bo
            drawFrame(g, Assets.get("animation_sheets/walk_prof_deadline.png"), (e.age() / 6) % 4, 0, 160, 192, x, y, w, h, tint);
        }
    }

    // ---------------- Dan, token, hieu ung, chu bay ----------------

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

    private void drawEffects(Graphics2D g, RenderSnapshot s) {
        Composite old = g.getComposite();
        for (EffectView e : s.effects()) {
            double p = e.age() / (double) e.total();                  // 0 -> 1
            switch (e.kind()) {
                case EXPLOSION -> {
                    // vung no 3x3: vong tron cam mo rong, mo dan
                    int cx = tileX(e.col()) + Constants.TILE / 2, cy = tileY(e.row()) + Constants.TILE / 2;
                    int r = (int) (Constants.TILE * 1.5 * (0.3 + 0.7 * p));
                    g.setComposite(AlphaComposite.SrcOver.derive((float) (0.65 * (1 - p))));
                    g.setColor(new Color(255, 170, 40));
                    g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
                    g.setComposite(old);
                    drawFrame(g, Assets.get("animation_sheets/charger_state_animation.png"),
                            Math.min(3, (int) (p * 4)), 3, 160, 192,
                            tileX(e.col()) + (Constants.TILE - Constants.SPRITE_W) / 2,
                            tileY(e.row()) + (Constants.TILE - Constants.SPRITE_H) / 2,
                            Constants.SPRITE_W, Constants.SPRITE_H, Tint.NONE);
                }
                case BEAM -> {
                    // tu tay Weaboo den mep phai cua ban
                    int x0 = (int) (Constants.GRID_X + (e.col() + 0.8) * Constants.TILE);
                    int x1 = Constants.GRID_X + Constants.COLS * Constants.TILE;
                    int cy = tileY(e.row()) + 42;
                    drawFrame(g, Assets.get("wifi_beam_effect_strip.png"), Math.min(3, (int) (p * 4)), 0, 256, 64,
                            x0, cy - 24, x1 - x0, 48, Tint.NONE);
                    g.drawImage(Assets.get("wifi_beam_head.png"), x1 - 40, cy - 20, 40, 40, null);
                }
            }
        }
        g.setComposite(old);
    }

    private void drawFloats(Graphics2D g, RenderSnapshot s) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        FontMetrics fm = g.getFontMetrics();
        for (FloatView f : s.floats()) {
            int a = (int) (255 * Math.max(0, Math.min(1, f.alpha())));
            int x = f.x() - fm.stringWidth(f.text()) / 2;
            g.setColor(new Color(0, 0, 0, a));
            g.drawString(f.text(), x + 1, f.y() + 1);
            g.setColor(new Color(255, 245, 160, a));
            g.drawString(f.text(), x, f.y());
        }
    }

    private void drawHealthBar(Graphics2D g, int x, int y, int w, double frac) {
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(x - 1, y - 1, w + 2, 6);
        g.setColor(frac > 0.5 ? new Color(90, 200, 90) : frac > 0.25 ? new Color(230, 190, 50) : new Color(220, 70, 60));
        g.fillRect(x, y, (int) Math.round(w * Math.max(0, frac)), 4);
    }

    // ---------------- Bottom bar, banner, overlay ----------------

    private void drawBottomBar(Graphics2D g, RenderSnapshot s) {
        g.setColor(new Color(20, 24, 30));
        g.fillRoundRect(90, 510, 720, 44, 12, 12);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        g.drawString(s.message(), 104, 538);

        if (!s.bossLabel().isEmpty()) {                            // dang danh boss: thanh mau boss thay thanh wave
            int x = 200, y = 560, w = 500, h = 20;
            g.setColor(new Color(20, 24, 30));
            g.fillRoundRect(x, y, w, h, 8, 8);
            g.setColor(new Color(200, 60, 60));
            g.fillRoundRect(x, y, (int) (w * Math.max(0, s.bossHealth())), h, 8, 8);
            g.setColor(Color.WHITE);
            g.drawRoundRect(x, y, w, h, 8, 8);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(s.bossLabel(), x + (w - fm.stringWidth(s.bossLabel())) / 2, y + 14);
        } else {
            g.setColor(new Color(20, 24, 30));                     // thanh tien do wave
            g.fillRect(90, 570, 720, 8);
            g.setColor(new Color(150, 100, 220));
            g.fillRect(90, 570, (int) (720 * s.waveProgress()), 8);
        }
    }

    private void drawBanner(Graphics2D g, RenderSnapshot s) {
        if (s.bannerAlpha() <= 0 || s.state() != GameState.PLAYING) return;
        int a = (int) (255 * s.bannerAlpha());
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
        int w = g.getFontMetrics().stringWidth(s.banner());
        int x = (Constants.WIN_W - w) / 2, y = 300;
        g.setColor(new Color(0, 0, 0, a * 2 / 3));
        g.drawString(s.banner(), x + 2, y + 2);
        g.setColor(new Color(255, 255, 255, a));
        g.drawString(s.banner(), x, y);
    }

    private void drawOverlay(Graphics2D g, RenderSnapshot s) {
        if (s.state() == GameState.PLAYING) return;
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, 0, Constants.WIN_W, Constants.WIN_H);
        g.setColor(Color.WHITE);
        switch (s.state()) {
            case PAUSED -> {
                drawCentered(g, "PAUSED", 250, new Font(Font.SANS_SERIF, Font.BOLD, 44));
                drawButton(g, s, Layout.PRIMARY, "Resume", true);
                drawButton(g, s, Layout.SECONDARY, "Restart stage", true);
                drawButton(g, s, Layout.TERTIARY, "Back to menu", true);
            }
            case WON -> {
                drawCentered(g, "Stage cleared!", 230, new Font(Font.SANS_SERIF, Font.BOLD, 44));
                drawCentered(g, s.message(), 280, new Font(Font.SANS_SERIF, Font.PLAIN, 18));
                drawButton(g, s, Layout.PRIMARY, "Next stage", true);
                drawButton(g, s, Layout.SECONDARY, "Back to menu", true);
            }
            case LOST -> {
                drawCentered(g, "Marked absent", 230, new Font(Font.SANS_SERIF, Font.BOLD, 44));
                drawCentered(g, s.message(), 280, new Font(Font.SANS_SERIF, Font.PLAIN, 18));
                drawButton(g, s, Layout.PRIMARY, "Retry", true);
                drawButton(g, s, Layout.SECONDARY, "Back to menu", true);
            }
            case ENDING -> {
                drawCentered(g, "COURSE PASSED", 230, new Font(Font.SANS_SERIF, Font.BOLD, 48));
                drawCentered(g, "...Fine. Everyone gets a B. Class dismissed.", 280, new Font(Font.SANS_SERIF, Font.PLAIN, 18));
                drawCentered(g, s.message(), 306, new Font(Font.SANS_SERIF, Font.PLAIN, 16));
                drawButton(g, s, Layout.PRIMARY, "Back to menu", true);
            }
            default -> { }
        }
    }

    // ---------------- Menu & chon stage ----------------

    private void drawMenu(Graphics2D g, RenderSnapshot s) {
        g.setColor(Color.WHITE);
        drawCentered(g, "Flu Season Defense", 130, new Font(Font.SANS_SERIF, Font.BOLD, 54));

        g.drawImage(Assets.get("placed/year1_student_placed.png"), 250, 150, 128, 144, null);
        g.drawImage(Assets.get("sick_student.png"), 522, 150, 128, 144, null);

        g.setColor(new Color(200, 205, 215));
        Font f = new Font(Font.SANS_SERIF, Font.ITALIC, 15);
        drawCentered(g, "Flu season hit campus. Midterms hit harder.", 296 + 18, f);
        drawCentered(g, "Your classmates are turning purple. Hold the lecture hall.", 296 + 38, f);

        drawButton(g, s, Layout.MENU_PLAY, "Play", true);
        drawButton(g, s, Layout.MENU_QUIT, "Quit", true);

        if (!s.error().isEmpty()) {                        // loi doc stage: hien o day thay vi crash
            g.setColor(new Color(255, 120, 110));
            drawCentered(g, "Could not load stage:", 500, new Font(Font.SANS_SERIF, Font.BOLD, 15));
            drawCentered(g, s.error(), 524, new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        }
    }

    private void drawStageSelect(Graphics2D g, RenderSnapshot s) {
        g.setColor(Color.WHITE);
        drawCentered(g, "Select stage", 100, new Font(Font.SANS_SERIF, Font.BOLD, 40));
        for (int i = 0; i < Stages.TITLES.length; i++) {
            boolean open = i <= s.unlocked();
            drawButton(g, s, Layout.stageButton(i), (i == 0 ? "Tutorial - " : "Stage " + i + " - ") + Stages.TITLES[i], open);
        }
        drawButton(g, s, Layout.SELECT_BACK, "Back", true);
    }

    // ---------------- Tien ich ve ----------------

    private void drawButton(Graphics2D g, RenderSnapshot s, Layout.Rect r, String label, boolean enabled) {
        boolean hover = enabled && r.contains(s.mouseX(), s.mouseY());
        g.setColor(!enabled ? new Color(70, 72, 78) : hover ? new Color(110, 150, 120) : new Color(70, 110, 85));
        g.fillRoundRect(r.x(), r.y(), r.w(), r.h(), 14, 14);
        g.setColor(enabled ? new Color(200, 230, 205) : new Color(100, 102, 108));
        g.drawRoundRect(r.x(), r.y(), r.w(), r.h(), 14, 14);
        g.setColor(enabled ? Color.WHITE : new Color(130, 132, 138));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label, r.x() + (r.w() - fm.stringWidth(label)) / 2, r.y() + (r.h() + fm.getAscent() - fm.getDescent()) / 2);
    }

    private void drawCentered(Graphics2D g, String text, int baselineY, Font font) {
        g.setFont(font);
        int w = g.getFontMetrics().stringWidth(text);
        g.drawString(text, (Constants.WIN_W - w) / 2, baselineY);
    }

    /** Ve 1 o (frame, row) cua sheet. WHITE = nhay trang, GREY = bi choang; to theo hinh bong, giu nen trong suot. */
    private void drawFrame(Graphics2D g, BufferedImage sheet, int frame, int row, int fw, int fh,
                           int x, int y, int w, int h, Tint tint) {
        if (tint == Tint.NONE) { Assets.drawFrame(g, sheet, frame, row, fw, fh, x, y, w, h); return; }
        BufferedImage tmp = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D tg = tmp.createGraphics();
        tg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        Assets.drawFrame(tg, sheet, frame, row, fw, fh, 0, 0, w, h);
        tg.setComposite(AlphaComposite.SrcAtop.derive(tint == Tint.WHITE ? 0.75f : 0.6f));
        tg.setColor(tint == Tint.WHITE ? Color.WHITE : new Color(110, 112, 122));
        tg.fillRect(0, 0, w, h);
        tg.dispose();
        g.drawImage(tmp, x, y, null);
    }

    private static boolean isStudent(UnitType t) {
        return t == UnitType.YEAR1 || t == UnitType.YEAR2 || t == UnitType.YEAR3 || t == UnitType.YEAR4;
    }

    private static int yearRow(UnitType t) {
        return switch (t) { case YEAR1 -> 0; case YEAR2 -> 1; case YEAR3 -> 2; default -> 3; };
    }

    private static int tileX(int col) { return Constants.GRID_X + col * Constants.TILE; }
    private static int tileY(int row) { return Constants.GRID_Y + row * Constants.TILE; }
}
