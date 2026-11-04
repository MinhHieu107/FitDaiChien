package game.entity;

import game.core.GameModel;

public abstract class Defender {
    private final UnitType type;
    private final int row, col;
    private double health;
    private int stunTicks = 0;

    protected Defender(UnitType type, int row, int col) {
        this.type = type;
        this.row = row;
        this.col = col;
        this.health = type.health;
    }

    /** Logic rieng cua tung loai. Chi duoc goi khi KHONG bi choang. */
    protected abstract void update(GameModel model);

    /** GameModel goi ham nay moi tick. Bi choang thi dung moi thu (ban, sinh token, dem gio charger). */
    public final void tick(GameModel model) {
        if (stunTicks > 0) { stunTicks--; return; }
        update(model);
    }

    public void stun(int ticks) { stunTicks = Math.max(stunTicks, ticks); }
    public boolean isStunned() { return stunTicks > 0; }

    public void takeDamage(double amount) { health = Math.max(0, health - amount); }
    /** Go khoi san (khong no, khong gi ca): dung khi charger da no, hoac bi pha huy. */
    public void remove() { health = 0; }
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
    /** Hang trong atlas animation rieng cua loai nay, hoac -1 neu dung sprite tinh. */
    public int visualRow() { return -1; }
    public int visualFrame() { return 0; }
}
