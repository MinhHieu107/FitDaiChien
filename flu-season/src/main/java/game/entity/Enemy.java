package game.entity;

import game.core.Constants;
import game.core.GameModel;

public class Enemy {
    public static final double WIDTH = 0.6;           // hitbox [x, x + 0.6]
    public static final int FLASH_TICKS = 3;          // nhay trang khi trung don
    public static final int FADE_TICKS = 10;          // mo dan khi chet

    private final EnemyType type;
    private final int row;
    private double x;
    private double health;
    private boolean biting = false;
    private int age = 0;
    private int flashTicks = 0;
    private int deathAge = -1;                         // -1 = con song; >= 0 = so tick ke tu luc chet

    public Enemy(EnemyType type, int row) { this(type, row, Constants.ENEMY_SPAWN_X); }

    public Enemy(EnemyType type, int row, double x) {
        this.type = type;
        this.row = row;
        this.x = x;
        this.health = type.health;
    }

    /** Buoc 6 cua tick: bi chan thi can, khong thi di sang trai. */
    public void update(GameModel model) {
        age++;
        if (flashTicks > 0) flashTicks--;
        if (deathAge >= 0) { deathAge++; return; }     // dang mo dan: khong di, khong can
        Defender blocker = model.blockerOf(this);
        biting = blocker != null;
        if (biting) blocker.takeDamage(bitePerSecond() / Constants.TPS);
        else x -= speedPerSecond(model) / Constants.TPS;
    }

    protected double speedPerSecond(GameModel model) { return type.speed * model.enemySpeedMultiplier(); }
    protected double bitePerSecond() { return type.bitePerSecond; }

    public void takeDamage(double amount) {
        if (isDead()) return;
        health = Math.max(0, health - amount);
        flashTicks = FLASH_TICKS;
        if (health <= 0) deathAge = 0;                 // bat dau mo dan
    }

    /** Het mau (co the van dang mo dan: khong bi ban, khong can, khong tinh vao thang/thua). */
    public boolean isDead() { return health <= 0; }
    /** Mo dan xong -> GameModel go khoi danh sach. */
    public boolean fadeDone() { return deathAge >= FADE_TICKS; }
    public boolean isFlashing() { return flashTicks > 0; }
    /** 1.0 = ro net, ve 0 khi chet. */
    public double alpha() { return deathAge < 0 ? 1.0 : Math.max(0.0, 1.0 - deathAge / (double) FADE_TICKS); }

    /** Chieu rong hitbox (boss rong hon). */
    public double width() { return WIDTH; }
    public double left() { return x; }
    public double right() { return x + width(); }

    // Thong tin them cho boss (ke dich thuong tra ve mac dinh)
    public boolean isBoss() { return false; }
    public int phaseIndex() { return 0; }
    public BossAction bossAction() { return null; }
    public int bossActionAge() { return 0; }

    public EnemyType type() { return type; }
    public int row() { return row; }
    public double x() { return x; }
    public double health() { return health; }
    public boolean isBiting() { return biting; }
    public int age() { return age; }
}
