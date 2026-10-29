package game.entity;

import game.core.Constants;
import game.core.GameModel;

/** Bay: MINE (0-6s) -> BOMB (6-14s) -> UNSTABLE (14-20s, het gio thi no). */
public class BrokenCharger extends Defender {
    public enum State { MINE, BOMB, UNSTABLE }

    private static final int BOMB_AT = 6 * 30, UNSTABLE_AT = 14 * 30, EXPLODE_AT = 20 * 30;
    private static final int MINE_DAMAGE = 600, BLAST_DAMAGE = 1800, FRIENDLY_DAMAGE = 400, BOSS_DAMAGE = 500;
    private static final int EXPLOSION_TICKS = 15;

    private int timer = 0;
    private State lastState = State.MINE;

    public BrokenCharger(int row, int col) { super(UnitType.CHARGER, row, col); }

    public State state() { return timer < BOMB_AT ? State.MINE : timer < UNSTABLE_AT ? State.BOMB : State.UNSTABLE; }
    public boolean canDetonateByClick() { return !isDead() && state() != State.MINE; }

    @Override protected void update(GameModel model) {
        if (isDead()) return;
        timer++;

        State s = state();
        if (s != lastState) {                                        // doi trang thai: hien cau thoai
            lastState = s;
            model.floatText(s == State.BOMB ? "Battery: 1%. Danger: 100%." : "Why is it hot?", pixelX(), pixelY());
        }

        Enemy toucher = model.firstEnemyTouching(row(), left(), right());
        if (toucher != null) explode(model, toucher);
        else if (timer >= EXPLODE_AT) explode(model, null);
    }

    /** Nguoi choi bam vao charger o trang thai BOMB hoac UNSTABLE. */
    public void detonateByClick(GameModel model) {
        if (canDetonateByClick()) explode(model, null);
    }

    private void explode(GameModel model, Enemy toucher) {
        if (isDead()) return;                                        // chi no 1 lan du nhieu dich cung cham
        State s = state();
        if (s == State.MINE) {
            if (toucher != null) hit(toucher, MINE_DAMAGE);          // chi con dich cham vao
        } else {
            int r0 = Math.max(0, row() - 1), r1 = Math.min(Constants.ROWS - 1, row() + 1);
            int c0 = Math.max(0, col() - 1), c1 = Math.min(Constants.COLS - 1, col() + 1);
            for (Enemy e : model.enemyList())
                if (!e.isDead() && e.row() >= r0 && e.row() <= r1 && e.left() < c1 + 1 && e.right() > c0)
                    hit(e, BLAST_DAMAGE);
            if (s == State.UNSTABLE) {                               // phan no tu gay thuong cho phe minh
                for (int r = r0; r <= r1; r++)
                    for (int c = c0; c <= c1; c++) {
                        Defender d = model.defenderAt(r, c);
                        if (d != null && d != this) d.takeDamage(FRIENDLY_DAMAGE);
                    }
            }
        }
        model.addEffect(Effect.Kind.EXPLOSION, row(), col(), EXPLOSION_TICKS);
        remove();
    }

    /** Voi boss, MOI vu no deu chi gay dung 500. */
    private void hit(Enemy e, int damage) { e.takeDamage(e.isBoss() ? BOSS_DAMAGE : damage); }

    private int pixelX() { return Constants.GRID_X + col() * Constants.TILE + Constants.TILE / 2; }
    private int pixelY() { return Constants.GRID_Y + row() * Constants.TILE + 8; }

    @Override public int actionAge() { return timer; }
    @Override public int visualRow() { return state().ordinal(); }          // hang 0/1/2 cua charger_state_animation
    @Override public int visualFrame() { return (timer / 6) % 4; }
}
