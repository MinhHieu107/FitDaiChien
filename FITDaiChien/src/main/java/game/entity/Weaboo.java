package game.entity;

import game.core.Constants;
import game.core.GameModel;

public class Weaboo extends Defender {
    private static final int INTERVAL = 75;              // 2.5 s
    private static final int PLUSHIE_GAP = 4;

    private int timer = INTERVAL;                        // san sang ngay khi co dich
    private WeabooAttack current = null;
    private int moveAge = 10_000;
    private int plushiesLeft = 0, plushieCooldown = 0;

    public Weaboo(int row, int col) { super(UnitType.WEABOO, row, col); }

    @Override protected void update(GameModel model) {
        if (moveAge < 10_000) moveAge++;

        if (plushiesLeft > 0 && --plushieCooldown <= 0) {     // vien plushie ke tiep trong loat
            throwPlushie(model);
            plushiesLeft--;
            plushieCooldown = PLUSHIE_GAP;
        }

        timer = Math.min(timer + 1, INTERVAL);
        if (timer >= INTERVAL && model.hasEnemyAhead(row(), col())) {
            timer = 0;
            current = WeabooAttacks.roll(model.rng().nextInt(100));   // 1 lan roll duy nhat
            moveAge = 0;
            model.floatText(current.displayName(),
                    Constants.GRID_X + col() * Constants.TILE + Constants.TILE / 2,
                    Constants.GRID_Y + row() * Constants.TILE + 8);
            current.execute(this, model);
        }
    }

    public void startPlushies(GameModel model) {
        throwPlushie(model);
        plushiesLeft = 2;
        plushieCooldown = PLUSHIE_GAP;
    }

    private void throwPlushie(GameModel model) {
        model.addProjectile(new Projectile(Projectile.Kind.PLUSHIE, row(), col() + 0.8, 15));
    }

    private boolean animating() { return current != null && moveAge < current.animationTicks(); }
    @Override public int actionAge() { return moveAge; }
    @Override public int visualRow() { return animating() ? current.sheetRow() : -1; }
    @Override public int visualFrame() { return animating() ? Math.min(3, moveAge * 4 / current.animationTicks()) : 0; }
}
