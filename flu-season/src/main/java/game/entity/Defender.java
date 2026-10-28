package game.entity;

import game.core.GameModel;

public abstract class Defender {
    private final UnitType type;
    private final int row, col;
    private int health;

    protected Defender(UnitType type, int row, int col) {
        this.type = type;
        this.row = row;
        this.col = col;
        this.health = type.health;
    }

    /** Duoc goi moi tick boi GameModel. Lop con tu quyet dinh lam gi. */
    public abstract void update(GameModel model);

    public void takeDamage(int amount) { health = Math.max(0, health - amount); }
    public boolean isDead() { return health <= 0; }

    public UnitType type() { return type; }
    public int row() { return row; }
    public int col() { return col; }
    public int health() { return health; }

    /** Cho animation: so tick tu lan cuoi "san xuat" (chi PC dung). */
    public int pulseAge() { return Integer.MAX_VALUE; }
}
