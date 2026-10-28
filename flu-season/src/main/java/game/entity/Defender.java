package game.entity;

import game.core.GameModel;

public abstract class Defender {
    private final UnitType type;
    private final int row, col;
    private double health;

    protected Defender(UnitType type, int row, int col) {
        this.type = type;
        this.row = row;
        this.col = col;
        this.health = type.health;
    }

    /** Duoc goi moi tick boi GameModel. Lop con tu quyet dinh lam gi. */
    public abstract void update(GameModel model);

    public void takeDamage(double amount) { health = Math.max(0, health - amount); }
    public boolean isDead() { return health <= 0; }

    public UnitType type() { return type; }
    public int row() { return row; }
    public int col() { return col; }
    public double health() { return health; }

    /** Hitbox ngang cua defender: [col + 0.1, col + 0.9]. */
    public double left() { return col + 0.1; }
    public double right() { return col + 0.9; }

    /** Cho animation: so tick ke tu hanh dong gan nhat (PC: sinh token, Student: ban). */
    public int actionAge() { return Integer.MAX_VALUE; }
}
