package game.entity;

import game.core.GameModel;

/** Prof. Deadline. Hitbox [x, x+1.0], 10000 mau, phase theo mau, cac don danh theo timer. */
public class Boss extends Enemy {
    public static final double WIDTH = 1.0;
    public static final double SPAWN_X = 9.8;

    private static final int CHALK_EVERY = 4 * 30, SUMMON_EVERY = 8 * 30, QUIZ_EVERY = 10 * 30;
    private static final int MOVED_UP_EVERY = 15 * 30, ATTENDANCE_EVERY = 20 * 30;

    private BossPhase phase = BossPhase.LECTURE;
    private int chalkTimer, summonTimer, quizTimer, movedUpTimer, attendanceTimer;
    private static final int MOVE_EVERY = 10 * 30, SLIDE_TICKS = 12;
    private int moveTimer, slideAge = SLIDE_TICKS, prevRow;
    private BossAction action = null;
    private int actionAge = 0;

    public Boss(int row) { super(EnemyType.PROF, row, SPAWN_X); prevRow = row; }

    @Override public void update(GameModel model) {
        actionAge++;
        if (action != null && action.ticks > 0 && actionAge >= action.ticks) action = null;
        super.update(model);                       // di / can / mo dan
        if (isDead()) return;

        syncPhase(model);

        if (slideAge < SLIDE_TICKS) slideAge++;
        if (action != BossAction.DEFEAT && ++moveTimer >= MOVE_EVERY) {   // ~10 s: nhay len/xuong 1 hang
            moveTimer = 0;
            int[] opts = new int[2]; int n = 0;
            for (int r : new int[]{row() - 1, row() + 1})
                if (r >= 0 && r < game.core.Constants.ROWS && model.rowActive(r)) opts[n++] = r;
            if (n > 0) { prevRow = row(); setRow(opts[model.rng().nextInt(n)]); slideAge = 0; }
        }

        // Timer chung: bat dau dem tu luc boss xuat hien
        if (++chalkTimer >= CHALK_EVERY) {
            chalkTimer = 0;
            model.addProjectile(new Projectile(Projectile.Kind.CHALK, row(), x(), 60));
            act(BossAction.CHALK);
        }
        if (++summonTimer >= SUMMON_EVERY) {
            summonTimer = 0;
            int[] rs = new int[game.core.Constants.ROWS]; int n = 0;            // moi hang dang dung, tru hang cua boss
            for (int r = 0; r < rs.length; r++) if (r != row() && model.rowActive(r)) rs[n++] = r;
            if (n > 0) model.spawnEnemy(new Enemy(EnemyType.SICK, rs[model.rng().nextInt(n)], x()));
            act(BossAction.SUMMON);
        }
        // Timer theo phase: bat dau dem tu luc vao phase do
        if (phase.ordinal() >= BossPhase.POP_QUIZ.ordinal() && ++quizTimer >= QUIZ_EVERY) {
            quizTimer = 0;
            model.popQuiz();
            act(BossAction.QUIZ);
        }
        if (phase == BossPhase.FINALS_RAGE) {
            if (++movedUpTimer >= MOVED_UP_EVERY) {
                movedUpTimer = 0;
                model.boostEnemies();
                act(BossAction.MOVED_UP);
            }
            if (++attendanceTimer >= ATTENDANCE_EVERY) {
                attendanceTimer = 0;
                model.pausePcs();
                act(BossAction.ATTENDANCE);
            }
        }
    }

    /** Doi phase theo mau; co the nhay nhieu phase 1 luc neu bi dam manh. */
    private void syncPhase(GameModel model) {
        BossPhase target = BossPhase.forHealth(health() / type().health);
        while (phase.ordinal() < target.ordinal()) {
            phase = BossPhase.values()[phase.ordinal() + 1];
            quizTimer = 0; movedUpTimer = 0; attendanceTimer = 0;
            model.onBossPhase(phase);
        }
    }

    @Override public double visualRow() {
        double t = Math.min(1.0, slideAge / (double) SLIDE_TICKS);
        return prevRow + (row() - prevRow) * t;
    }

    private void act(BossAction a) { action = a; actionAge = 0; }
    public void startDefeat() { act(BossAction.DEFEAT); }

    @Override protected double speedPerSecond(GameModel model) {
        return phase == BossPhase.FINALS_RAGE ? 0.20 : 0.10;           // khong bi "Deadline Moved Up" anh huong
    }
    @Override public double width() { return WIDTH; }
    @Override public boolean fadeDone() { return false; }              // boss o lai cho den man ket thuc
    @Override public double alpha() { return 1.0; }
    @Override public boolean isBoss() { return true; }
    @Override public int phaseIndex() { return phase.ordinal(); }
    @Override public BossAction bossAction() { return action; }
    @Override public int bossActionAge() { return actionAge; }
    public BossPhase phase() { return phase; }
}
