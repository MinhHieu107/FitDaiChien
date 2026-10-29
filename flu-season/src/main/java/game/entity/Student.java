package game.entity;

import game.core.Constants;
import game.core.GameModel;

/** Year 1..4: cung mot lop, chi khac so vien dan va toc do (bang chi so ben duoi). */
public class Student extends Defender {
    private final int ballsPerShot;
    private final int intervalTicks;

    private int timer;                 // dem len lien tuc, dung o intervalTicks khi chua co muc tieu
    private int ballsLeft = 0;         // so vien con phai ban trong loat hien tai
    private int ballCooldown = 0;
    private int sinceShot = Integer.MAX_VALUE;

    public Student(UnitType type, int row, int col) {
        super(type, row, col);
        switch (type) {
            case YEAR1 -> { ballsPerShot = 1; intervalTicks = 45; }   // 1.5 s
            case YEAR2 -> { ballsPerShot = 2; intervalTicks = 45; }   // 1.5 s
            case YEAR3 -> { ballsPerShot = 3; intervalTicks = 36; }   // 1.2 s
            case YEAR4 -> { ballsPerShot = 4; intervalTicks = 30; }   // 1.0 s
            default -> throw new IllegalArgumentException("Khong phai Student: " + type);
        }
        this.timer = intervalTicks;    // san sang ban ngay khi co muc tieu
    }

    @Override protected void update(GameModel model) {
        if (sinceShot < 1000) sinceShot++;
        timer = Math.min(timer + 1, intervalTicks);

        // dang ban do mot loat: moi vien cach nhau 3 tick
        if (ballsLeft > 0 && --ballCooldown <= 0) {
            spawnBall(model);
            ballsLeft--;
            ballCooldown = Constants.BALL_GAP_TICKS;
        }
        // bat dau loat moi
        if (timer >= intervalTicks && ballsLeft == 0 && model.hasEnemyAhead(row(), col())) {
            timer = 0;
            sinceShot = 0;
            spawnBall(model);
            ballsLeft = ballsPerShot - 1;
            ballCooldown = Constants.BALL_GAP_TICKS;
        }
    }

    private void spawnBall(GameModel model) {
        // Year 4 gay gap doi sat thuong len boss (40 thay vi 20)
        double vsBoss = type() == UnitType.YEAR4 ? 40 : 20;
        model.addProjectile(new Projectile(Projectile.Kind.PAPER, row(), col() + 0.8, 20, vsBoss));
    }

    @Override public int actionAge() { return sinceShot; }
}
