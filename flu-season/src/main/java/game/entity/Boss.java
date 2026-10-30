package game.entity;

import game.core.GameModel;

/** Prof. Deadline. Hitbox [x, x+1.0], 12000 mau, phase theo mau, cac don danh theo timer. */
public class Boss extends Enemy {
    public static final double WIDTH = 1.0;
    public static final double SPAWN_X = 9.8;

    private static final int CHALK_EVERY = 4 * 30, SUMMON_EVERY = 8 * 30, QUIZ_EVERY = 10 * 30;
    private static final int MOVED_UP_EVERY = 15 * 30, ATTENDANCE_EVERY = 20 * 30;
    private static final int[] SUMMON_ROWS = {0, 1, 3, 4};            // hang 1,2,4,5 (khong phai hang cua boss)

    private BossPhase phase = BossPhase.LECTURE;
    private int chalkTimer, summonTimer, quizTimer, movedUpTimer, attendanceTimer;
    private BossAction action = null;
    private int actionAge = 0;

    public Boss(int row) { super(EnemyType.PROF, row, SPAWN_X); }

    @Override public void update(GameModel model) {
        actionAge++;
        if (action != null && action.ticks > 0 && actionAge >= action.ticks) action = null;
        super.update(model);                       // di / can / mo dan
        if (isDead()) return;

        syncPhase(model);

        // Timer chung: bat dau dem tu luc boss xuat hien
        if (++chalkTimer >= CHALK_EVERY) {
            chalkTimer = 0;
            model.addProjectile(new Projectile(Projectile.Kind.CHALK, row(), x(), 60));
            act(BossAction.CHALK);
        }
        if (++summonTimer >= SUMMON_EVERY) {
            summonTimer = 0;
            int row = SUMMON_ROWS[model.rng().nextInt(SUMMON_ROWS.length)];
            model.spawnEnemy(new Enemy(EnemyType.SICK, row, x()));
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
