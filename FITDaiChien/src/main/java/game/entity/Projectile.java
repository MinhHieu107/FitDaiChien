package game.entity;

import game.core.Constants;

public class Projectile {
    public enum Kind {
        //     toc do (o/giay)  huong (+1 sang phai)  file anh
        PAPER   (5.0,  1, "paper_ball.png"),
        PLUSHIE (4.0,  1, "plushie.png"),
        CHALK   (4.0, -1, "chalk.png");

        public final double speed;
        public final int dir;
        public final String sprite;
        Kind(double speed, int dir, String sprite) { this.speed = speed; this.dir = dir; this.sprite = sprite; }
    }

    public static final double WIDTH = 0.2;           // hitbox [px, px + 0.2]

    private final Kind kind;
    private final int row;
    private final double damage;
    private final double bossDamage;                  // Year 4: gap doi khi trung boss
    private double px;

    public Projectile(Kind kind, int row, double px, double damage) { this(kind, row, px, damage, damage); }

    public Projectile(Kind kind, int row, double px, double damage, double bossDamage) {
        this.kind = kind; this.row = row; this.px = px; this.damage = damage; this.bossDamage = bossDamage;
    }

    public void update() { px += kind.dir * kind.speed / Constants.TPS; }

    public boolean isOut() { return px > 9.5 || px < -0.5; }

    public double left() { return px; }
    public double right() { return px + WIDTH; }
    public Kind kind() { return kind; }
    public int row() { return row; }
    public double px() { return px; }
    public double damage() { return damage; }
    public double bossDamage() { return bossDamage; }
}
