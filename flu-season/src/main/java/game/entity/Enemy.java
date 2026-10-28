package game.entity;

import game.core.Constants;
import game.core.GameModel;

public class Enemy {
    public static final double WIDTH = 0.6;           // hitbox [x, x + 0.6]

    private final EnemyType type;
    private final int row;
    private double x = Constants.ENEMY_SPAWN_X;
    private double health;
    private boolean biting = false;
    private int age = 0;

    public Enemy(EnemyType type, int row) {
        this.type = type;
        this.row = row;
        this.health = type.health;
    }

    /** Buoc 6 cua tick: bi chan thi can, khong thi di sang trai. */
    public void update(GameModel model) {
        age++;
        Defender blocker = model.blockerOf(this);
        biting = blocker != null;
        if (biting) blocker.takeDamage(type.bitePerSecond / (double) Constants.TPS);
        else x -= type.speed / Constants.TPS;
    }

    public void takeDamage(double amount) { health = Math.max(0, health - amount); }
    public boolean isDead() { return health <= 0; }

    public double left() { return x; }
    public double right() { return x + WIDTH; }

    public EnemyType type() { return type; }
    public int row() { return row; }
    public double x() { return x; }
    public double health() { return health; }
    public boolean isBiting() { return biting; }
    public int age() { return age; }
}
