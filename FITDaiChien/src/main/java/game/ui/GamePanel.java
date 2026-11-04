package game.ui;

import game.core.*;
import game.core.RenderSnapshot.*;
import game.entity.BossAction;
import game.entity.Effect;
import game.entity.UnitType;
import game.stage.Stages;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class GamePanel extends JPanel {
    private static final Color BG = new Color(40, 44, 52);
    private enum Tint { NONE, WHITE, GREY }

    private volatile RenderSnapshot snap = RenderSnapshot.empty();
    private volatile GameState previousState = GameState.MENU;

    public GamePanel() { setPreferredSize(new Dimension(Constants.WIN_W, Constants.WIN_H)); }

    /** Goi tu game thread. repaint() an toan khi goi tu thread khac. */
    public void publish(RenderSnapshot s) {
        GameState old = previousState;
        previousState = s.state();
        snap = s;
        repaint();
    }

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
            case HELP -> drawHelp(g, s);
            case INTRO -> drawIntro(g, s);
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
                drawTutorialHints(g, s);
                drawBottomBar(g, s);
                drawBanner(g, s);
                drawOverlay(g, s);
                if (s.state() == GameState.PLAYING) drawDigCursor(g, s);
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
        drawDigButton(g, s);
    }

    // ---------------- Luoi ----------------

    /** May tinh (trai) va con duong (phai) bao quanh luoi. Ca hai la anh 90x240 gian ra 90x400 de khop 5 hang. */
    private void drawSurroundings(Graphics2D g, RenderSnapshot s) {
        int h = Constants.ROWS * Constants.TILE, roadX = Constants.GRID_X + Constants.COLS * Constants.TILE;
        g.drawImage(Assets.get("supercomputer.png"), 0, Constants.GRID_Y, Constants.GRID_X, h, null);
        g.drawImage(Assets.get("road.png"), roadX, Constants.GRID_Y, Constants.WIN_W - roadX, h, null);
        for (int r = 0; r < Constants.ROWS; r++)               // lan khong dung duoc: toi di nhu o luoi
            if (!s.activeRows().get(r)) {
                g.setColor(new Color(30, 30, 34, 170));
                g.fillRect(roadX, tileY(r), Constants.WIN_W - roadX, Constants.TILE);
            }
        for (EffectView e : s.effects())
            if (e.kind() == Effect.Kind.BREACH) {              // may tinh bi tan cong: nhap nhay do
                g.setColor(new Color(255, 50, 30, 50 + (int) (70 * (0.5 + 0.5 * Math.sin(e.age() * 0.8)))));
                g.fillRect(0, Constants.GRID_Y, Constants.GRID_X, h);
            }
    }

    private void drawGrid(Graphics2D g, RenderSnapshot s) {
        drawSurroundings(g, s);
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
        if (s.hoverRow() < 0) return;
        String f;
        if (s.digging()) {
            if (!s.hoverValid()) return;                          // o trong: khong co gi de go
            f = "tile_placement_invalid.png";                     // o do = quan se bi go
        } else if (s.selected() != null) {
            f = s.hoverValid() ? "tile_placement_valid.png" : "tile_placement_invalid.png";
        } else return;
        g.drawImage(Assets.get(f), tileX(s.hoverCol()), tileY(s.hoverRow()), Constants.TILE, Constants.TILE, null);
    }

    // ---------------- Nut con gian ----------------

    private void drawDigButton(Graphics2D g, RenderSnapshot s) {
        Layout.Rect r = Layout.DIG_BTN;
        boolean on = s.digging(), hover = r.contains(s.mouseX(), s.mouseY());
        g.setColor(on ? new Color(150, 90, 50) : hover ? new Color(110, 150, 120) : new Color(70, 110, 85));
        g.fillRoundRect(r.x(), r.y(), r.w(), r.h(), 14, 14);
        g.setColor(on ? new Color(255, 210, 150) : new Color(200, 230, 205));
        g.drawRoundRect(r.x(), r.y(), r.w(), r.h(), 14, 14);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(Assets.get("conGian.png"), r.x() + (r.w() - 50) / 2, r.y() + 10, 50, 50, null);  // 800/50 = 16: dung luoi pixel
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        String label = on ? "Cancel" : "Remove";
        g.drawString(label, r.x() + (r.w() - g.getFontMetrics().stringWidth(label)) / 2, r.y() + r.h() - 12);
    }

    /** Con gian di theo chuot khi dang o che do go quan. */
    private void drawDigCursor(Graphics2D g, RenderSnapshot s) {
        if (!s.digging() || s.mouseX() < 0) return;
        g.setComposite(AlphaComposite.SrcOver.derive(0.85f));
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(Assets.get("conGian.png"), s.mouseX() + 6, s.mouseY() + 6, 50, 50, null);
        g.setComposite(AlphaComposite.SrcOver);
    }

    // ---------------- Defender ----------------

    private void drawDefenders(Graphics2D g, RenderSnapshot s) {
        for (DefenderView d : s.defenders()) {
            int x = tileX(d.col()) + (Constants.TILE - Constants.SPRITE_W) / 2;
            int y = tileY(d.row()) + (Constants.TILE - Constants.SPRITE_H) / 2;
            UnitType t = d.type();
            Tint tint = d.stunned() ? Tint.GREY : Tint.NONE;
            int w = Constants.SPRITE_W, h = Constants.SPRITE_H;

            boolean idle = !(t == UnitType.PC && d.actionAge() < 12) && !(isStudent(t) && d.actionAge() < 12)
                    && t != UnitType.CHARGER && !(t == UnitType.WEABOO && d.visualRow() >= 0)
                    && !(t == UnitType.GAMER && d.health() * 2 < t.health);
            if (!idle) g.drawImage(Assets.shadow("placed/" + t.sprite + ".png"), x, y, w, h, null);   // sheet hoat anh khong co bong

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
        int y = (int) (Constants.GRID_Y + e.rowF() * Constants.TILE) + 76 - h;                       // chan boss ngang chan cac nhan vat khac
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
                case BREACH -> {
                    int frame = Math.min(5, e.age() * 6 / e.total());          // 6 frame 64x64: tia lua -> no -> khoi
                    int size = 144, cx = 52, cy = tileY(e.row()) + Constants.TILE / 2;
                    Assets.drawFrame(g, Assets.get("computer_explosion_strip.png"), frame, 0, 64, 64,
                            cx - size / 2, cy - size / 2, size, size);
                }
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
        boolean guide = s.tutorialStep() > 0 && (s.state() == GameState.PLAYING || s.state() == GameState.PAUSED);
        if (guide) {                                               // khung huong dan cua tutorial: to hon, co nhan "STEP n/4"
            g.setColor(new Color(20, 24, 30));
            g.fillRoundRect(90, 502, 720, 62, 12, 12);
            g.setColor(new Color(255, 214, 64));
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(90, 502, 720, 62, 12, 12);
            g.setStroke(new BasicStroke(1));
            g.fillRoundRect(102, 513, 76, 24, 8, 8);
            g.setColor(new Color(30, 24, 10));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            g.drawString("STEP " + s.tutorialStep() + "/4", 111, 530);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
            drawWrapped(g, s.message(), 194, 525, 600, 20);
        } else {
            g.setColor(new Color(20, 24, 30));
            g.fillRoundRect(90, 510, 720, 44, 12, 12);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
            g.drawString(s.message(), 104, 538);
        }

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
            case WON -> drawVictoryOverlay(g, s, false);
            case LOST -> {
                drawCentered(g, "Marked absent", 230, new Font(Font.SANS_SERIF, Font.BOLD, 44));
                drawCentered(g, s.message(), 280, new Font(Font.SANS_SERIF, Font.PLAIN, 18));
                drawButton(g, s, Layout.PRIMARY, "Retry", true);
                drawButton(g, s, Layout.SECONDARY, "Back to menu", true);
            }
            case ENDING -> drawVictoryOverlay(g, s, true);
            default -> { }
        }
    }

    /** Man chien thang: nen toi, glow, confetti va bang vinh danh. */
    private void drawVictoryOverlay(Graphics2D g, RenderSnapshot s, boolean finalWin) {
        int w = Constants.WIN_W, h = Constants.WIN_H;
        g.setPaint(new GradientPaint(0, 0, new Color(8, 18, 42, 238), 0, h,
                new Color(37, 19, 65, 242)));
        g.fillRect(0, 0, w, h);

        // Anh sang toa tu tam man hinh
        for (int i = 5; i >= 0; i--) {
            g.setColor(new Color(255, 207, 92, 8 + i * 2));
            int rw = 280 + i * 75, rh = 210 + i * 65;
            g.fillOval((w - rw) / 2, 35 - i * 18, rw, rh);
        }

        // Confetti dung tick cua game de animation on dinh, khong can timer rieng
        Color[] confetti = { new Color(255, 213, 79), new Color(91, 224, 190),
                new Color(255, 112, 140), new Color(128, 171, 255), Color.WHITE };
        for (int i = 0; i < 72; i++) {
            int x = Math.floorMod(i * 137 + (int) (s.tick() * (i % 3 + 1)), w);
            int y = Math.floorMod(i * 71 + (int) (s.tick() * (i % 4 + 1)), h);
            g.setColor(confetti[i % confetti.length]);
            if (i % 3 == 0) g.fillOval(x, y, 5, 5);
            else g.fillRoundRect(x, y, 5, 10, 3, 3);
        }

        // Huy hieu
        int pulse = (int) (4 * Math.sin(s.tick() * 0.12));
        g.setColor(new Color(255, 203, 70, 45));
        g.fillOval(w / 2 - 62 - pulse, 38 - pulse, 124 + pulse * 2, 124 + pulse * 2);
        g.setColor(new Color(255, 213, 92));
        g.fillOval(w / 2 - 49, 49, 98, 98);
        g.setColor(new Color(143, 83, 18));
        g.setStroke(new BasicStroke(3f));
        g.drawOval(w / 2 - 41, 57, 82, 82);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 56));
        g.setColor(new Color(105, 65, 20));
        String star = "★";
        g.drawString(star, w / 2 - g.getFontMetrics().stringWidth(star) / 2, 119);

        // Bang vinh danh
        int px = 145, py = 170, pw = 610, ph = 315;
        g.setColor(new Color(0, 0, 0, 95));
        g.fillRoundRect(px + 7, py + 10, pw, ph, 30, 30);
        g.setPaint(new GradientPaint(px, py, new Color(36, 50, 83, 248), px, py + ph,
                new Color(19, 29, 54, 248)));
        g.fillRoundRect(px, py, pw, ph, 30, 30);
        g.setColor(new Color(255, 213, 92, 210));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(px, py, pw, ph, 30, 30);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, finalWin ? 37 : 39));
        g.setColor(new Color(255, 218, 111));
        drawCentered(g, finalWin ? "COURSE COMPLETED!" : "STAGE CLEARED!", 235, g.getFont());
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        g.setColor(Color.WHITE);
        drawCentered(g, finalWin ? "YOU SURVIVED THE WHOLE SEMESTER" : "VICTORY IS YOURS", 270, g.getFont());
        g.setColor(new Color(209, 220, 244));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        drawCentered(g, finalWin ? "The class is safe. Time to celebrate!" : s.message(), 298, g.getFont());

        // Nut nam trong vi tri hit-test da co san trong Layout
        if (finalWin) {
            drawButton(g, s, Layout.PRIMARY, "Back to menu", true);
        } else {
            drawButton(g, s, Layout.PRIMARY, "Next stage  →", true);
            drawButton(g, s, Layout.SECONDARY, "Back to menu", true);
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        g.setColor(new Color(190, 204, 231));
        drawCentered(g, "FIT Dai Chien  •  WELL PLAYED", 520, g.getFont());
    }

    // ---------------- Menu & chon stage ----------------

    private void drawMenu(Graphics2D g, RenderSnapshot s) {
        g.setColor(Color.WHITE);
        drawCentered(g, "FIT Dai Chien", 130, new Font(Font.SANS_SERIF, Font.BOLD, 54));

        g.drawImage(Assets.get("placed/year1_student_placed.png"), 250, 150, 128, 144, null);
        g.drawImage(Assets.get("sick_student.png"), 522, 150, 128, 144, null);

        g.setColor(new Color(200, 205, 215));
        Font f = new Font(Font.SANS_SERIF, Font.ITALIC, 15);
        drawCentered(g, "Deadline season hit campus. Midterms hit harder.", 296 + 18, f);
        drawCentered(g, "Your classmates are turning purple. Hold the lecture hall.", 296 + 38, f);

        drawButton(g, s, Layout.MENU_PLAY, "Play", true);
        drawButton(g, s, Layout.MENU_HELP, "How to Play", true);
        drawButton(g, s, Layout.MENU_QUIT, "Quit", true);

        if (!s.error().isEmpty()) {                        // loi doc stage: hien o day thay vi crash
            g.setColor(new Color(255, 120, 110));
            drawCentered(g, "Could not load stage:", 566, new Font(Font.SANS_SERIF, Font.BOLD, 15));
            drawCentered(g, s.error(), 588, new Font(Font.SANS_SERIF, Font.PLAIN, 14));
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

    // ---------------- Tutorial: danh dau cho can bam ----------------

    private void drawTutorialHints(Graphics2D g, RenderSnapshot s) {
        var target = s.tutorialTarget();
        if (target == RenderSnapshot.TutorialTarget.NONE || s.state() != GameState.PLAYING) return;
        double pulse = 0.5 + 0.5 * Math.sin(s.tick() * 0.2);
        int bounce = (int) (6 * pulse);
        Stroke oldStroke = g.getStroke();
        Composite oldComp = g.getComposite();
        switch (target) {
            case CARD_PC, CARD_YEAR1 -> {
                UnitType want = target == RenderSnapshot.TutorialTarget.CARD_PC ? UnitType.PC : UnitType.YEAR1;
                for (int i = 0; i < s.cards().size(); i++) {
                    if (s.cards().get(i).type() != want) continue;
                    int x = Constants.CARD_X + i * Constants.CARD_STEP, y = Constants.CARD_Y;
                    g.setStroke(new BasicStroke(4));
                    g.setColor(new Color(255, 214, 64, (int) (140 + 115 * pulse)));
                    g.drawRoundRect(x - 3, y - 3, Constants.CARD_W + 6, Constants.CARD_H + 6, 10, 10);
                }
            }
            case TILES -> {
                boolean[][] taken = new boolean[Constants.ROWS][Constants.COLS];
                for (DefenderView d : s.defenders()) taken[d.row()][d.col()] = true;
                g.setColor(new Color(255, 214, 64, (int) (45 + 70 * pulse)));
                for (int r = 0; r < Constants.ROWS; r++) {
                    if (!s.activeRows().get(r)) continue;
                    for (int c = 0; c < Constants.COLS; c++)
                        if (!taken[r][c]) g.fillRect(tileX(c), tileY(r), Constants.TILE, Constants.TILE);
                }
            }
            case TOKEN -> {
                for (TokenView t : s.tokens()) {
                    g.setStroke(new BasicStroke(3));
                    g.setColor(new Color(255, 214, 64, (int) (140 + 115 * pulse)));
                    g.drawOval(t.x() - 5, t.y() - 5, Constants.TOKEN_SIZE + 10, Constants.TOKEN_SIZE + 10);
                    drawArrow(g, t.x() + Constants.TOKEN_SIZE / 2, t.y() - 10 - bounce, false);
                }
            }
            default -> { }
        }
        g.setStroke(oldStroke);
        g.setComposite(oldComp);
    }

    /** Mui ten vang 18x14; pointUp = dinh huong len (chi vao vat o phia tren), nguoc lai huong xuong. */
    private void drawArrow(Graphics2D g, int cx, int y, boolean pointUp) {
        int[] xs = {cx, cx - 9, cx + 9};
        int[] ys = pointUp ? new int[]{y, y + 14, y + 14} : new int[]{y, y - 14, y - 14};
        g.setColor(new Color(255, 214, 64));
        g.fillPolygon(xs, ys, 3);
        g.setColor(new Color(60, 40, 0));
        g.drawPolygon(xs, ys, 3);
    }

    // ---------------- Man gioi thieu (truoc stage 0) ----------------

    private void drawIntro(Graphics2D g, RenderSnapshot s) {
        g.setColor(new Color(8, 10, 16));
        g.fillRect(0, 0, Constants.WIN_W, Constants.WIN_H);
        int page = s.introPage(), ticks = s.introTicks();
        float fade = Math.min(1f, ticks / 24f);                           // moi trang hien dan tu den
        Composite old = g.getComposite();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        g.setComposite(AlphaComposite.SrcOver.derive(fade));
        drawIntroArt(g, page, ticks);
        g.setComposite(old);

        // chu: ngat dong tren ca doan, chi ve phan da "danh" -> dong khong bi nhay khi chu hien them
        String full = Intro.PAGES[page];
        boolean quote = page == 1;
        Font font = new Font(Font.SERIF, quote ? Font.ITALIC : Font.PLAIN, 24);
        g.setFont(font);
        g.setColor(new Color(235, 232, 220));
        FontMetrics fm = g.getFontMetrics();
        java.util.List<String> lines = wrapLines(full, fm, 700);
        int remaining = Intro.visibleChars(page, ticks);
        int y = 350 + (lines.size() <= 2 ? 20 : 0);
        for (String line : lines) {
            if (remaining <= 0) break;
            String shown = line.substring(0, Math.min(line.length(), remaining));
            g.drawString(shown, (Constants.WIN_W - fm.stringWidth(line)) / 2, y);   // can giua theo dong day du
            remaining -= line.length() + 1;
            y += 34;
        }

        if (Intro.complete(page, ticks) && (ticks / 20) % 2 == 0) {
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            g.setColor(new Color(150, 156, 170));
            String hint = page == Intro.PAGES.length - 1 ? "Click or press Space to begin" : "Click or press Space to continue";
            drawCentered(g, hint, 548, g.getFont());
        }
        for (int i = 0; i < Intro.PAGES.length; i++) {                      // cham tien do
            g.setColor(i == page ? new Color(255, 214, 64) : new Color(70, 74, 86));
            g.fillOval(Constants.WIN_W / 2 - Intro.PAGES.length * 9 + i * 18, 566, 10, 10);
        }
        drawButton(g, s, Layout.INTRO_SKIP, "Skip >>", true);
    }

    /** Hinh minh hoa moi trang (cung phong cach pixel nhu game). */
    private void drawIntroArt(Graphics2D g, int page, int ticks) {
        int bob = (int) (4 * Math.sin(ticks * 0.1));
        switch (page) {
            case 0 -> {                                                     // hoc ky yen binh: ca lop ngoi cung nhau
                String[] row = {"year1_student", "pc", "gamer", "weaboo", "year2_student"};
                for (int i = 0; i < row.length; i++)
                    g.drawImage(Assets.get("placed/" + row[i] + "_placed.png"), 105 + i * 145, 110, 128, 154, null);
                g.setColor(new Color(60, 66, 80));
                g.fillRect(90, 268, 720, 3);
            }
            case 1 -> {                                                     // giao su Deadline
                g.setColor(new Color(255, 214, 64, 40));
                g.fillOval(330, 90, 240, 200);
                g.drawImage(Assets.get("placed/prof_deadline_placed.png"), 354, 80 + bob, 192, 230, null);
            }
            case 2 -> {                                                     // hoang loan: sinh vien tim
                g.setColor(new Color(220, 40, 40, 38));
                g.fillRect(0, 70, Constants.WIN_W, 240);
                String[] row = {"sick_student", "sick_student_tissue", "sick_student_blanket"};
                for (int i = 0; i < row.length; i++)
                    g.drawImage(Assets.get("placed/" + row[i] + "_placed.png"), 190 + i * 190, 100 + (i % 2 == 0 ? bob : -bob), 150, 180, null);
            }
            default -> {                                                    // nhiem vu: bao ve phong may chu bang doi hinh
                g.drawImage(Assets.get("supercomputer.png"), 40, 85, 90, 215, null);
                UnitType[] team = {UnitType.PC, UnitType.YEAR1, UnitType.YEAR2, UnitType.YEAR3,
                        UnitType.YEAR4, UnitType.GAMER, UnitType.WEABOO, UnitType.CHARGER};
                for (int i = 0; i < team.length; i++)
                    Assets.drawFrame(g, Assets.get("unit_cards.png"), team[i].cardCol, team[i].cardRow, 144, 184,
                            170 + i * 84, 150 + (i % 2 == 0 ? 0 : 20), 72, 90);
            }
        }
    }

    private java.util.List<String> wrapLines(String text, FontMetrics fm, int maxW) {
        java.util.List<String> out = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() > 0 && fm.stringWidth(line + " " + word) > maxW) { out.add(line.toString()); line.setLength(0); }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }

    // ---------------- Man How to Play ----------------

    private void drawHelp(Graphics2D g, RenderSnapshot s) {
        g.setColor(Color.WHITE);
        drawCentered(g, "How to Play", 62, new Font(Font.SANS_SERIF, Font.BOLD, 38));

        Font head = new Font(Font.SANS_SERIF, Font.BOLD, 17), body = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
        Color gold = new Color(255, 214, 64), text = new Color(215, 220, 228);
        int x = 50, w = 390, y = 104;

        String[][] left = {
                {"Goal", "Sick students walk in from the right. If one reaches the left edge of the classroom, you are marked absent."},
                {"Credits", "PCs make credit coins. Click a coin to collect it, then spend credits on student cards."},
                {"Placing students", "Click a card, then click an empty tile. Cards have a cooldown. Esc cancels your selection, P pauses."},
                {"Cockroach", "Click the cockroach button next to your credits, then click a student to remove him. No refund."},
        };
        for (String[] sec : left) {
            g.setFont(head); g.setColor(gold);
            g.drawString(sec[0], x, y);
            if (sec[0].equals("Cockroach"))                         // icon canh tieu de
                g.drawImage(Assets.get("conGian.png"), x + g.getFontMetrics().stringWidth(sec[0]) + 8, y - 16, 22, 22, null);
            g.setFont(body); g.setColor(text);
            y = drawWrapped(g, sec[1], x, y + 20, w, 18) + 8;
        }

        g.setFont(head); g.setColor(gold);
        g.drawString("Enemies", x, y + 4);
        String[] icons = {"sick_student.png", "sick_student_tissue.png", "sick_student_blanket.png", "prof_deadline.png"};
        String[] enemyNames = {"Sick", "Tissue (fast)", "Blanket (tough)", "Prof (boss)"};
        for (int i = 0; i < icons.length; i++) {
            int ix = x + i * 98;
            g.drawImage(Assets.get(icons[i]), ix + 20, y + 12, 44, 53, null);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12)); g.setColor(text);
            g.drawString(enemyNames[i], ix + 6, y + 80);
        }

        // cot phai: danh sach quan
        int rx = 480, ry = 104;
        g.setFont(head); g.setColor(gold);
        g.drawString("Your students", rx, ry);
        UnitType[] order = {UnitType.PC, UnitType.YEAR1, UnitType.YEAR2, UnitType.YEAR3, UnitType.YEAR4,
                UnitType.GAMER, UnitType.WEABOO, UnitType.CHARGER};
        String[] names = {"PC", "Year 1", "Year 2", "Year 3", "Year 4", "Gamer", "Weaboo", "Broken Charger"};
        String[] desc = {
                "Produces credit coins. Click them!",
                "Throws one paper ball at a time at enemies in his row.",
                "Throws 2 paper balls per volley.",
                "3 balls per volley, and fires faster.",
                "4 balls, the fastest shooter. Double damage to the professor.",
                "Tough wall: soaks up bites so your shooters survive.",
                "Random attacks: plushies, smash, a pose, or a Wi-Fi beam.",
                "Grows: mine (touch) -> bomb (click it: 3x3 blast) -> unstable (hurts your students too!)",
        };
        for (int i = 0; i < order.length; i++) {
            UnitType t = order[i];
            int cy = ry + 12 + i * 50;
            Assets.drawFrame(g, Assets.get("unit_cards.png"), t.cardCol, t.cardRow, 144, 184, rx, cy, 36, 46);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14)); g.setColor(Color.WHITE);
            g.drawString(names[i] + "  (" + t.cost + ")", rx + 46, cy + 14);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12)); g.setColor(text);
            drawWrapped(g, desc[i], rx + 46, cy + 30, 330, 14);
        }
        drawButton(g, s, Layout.HELP_BACK, "Back", true);
    }

    /** Ngat dong theo do rong; tra ve toa do baseline cua dong KE TIEP. Dung font/mau dang chon. */
    private int drawWrapped(Graphics2D g, String text, int x, int y, int maxW, int lineH) {
        FontMetrics fm = g.getFontMetrics();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() > 0 && fm.stringWidth(line + " " + word) > maxW) {
                g.drawString(line.toString(), x, y);
                y += lineH;
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) { g.drawString(line.toString(), x, y); y += lineH; }
        return y;
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
