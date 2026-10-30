package game.core;

import game.entity.BossAction;
import game.entity.Effect;
import game.entity.EnemyType;
import game.entity.Projectile;
import game.entity.UnitType;
import java.util.List;

/** Ban chup bat bien. Swing chi doc cai nay, khong cham vao model. */
public record RenderSnapshot(
        GameState state, long tick, int mouseX, int mouseY,
        int hoverRow, int hoverCol, boolean hoverValid,
        int credits, UnitType selected, boolean digging, String message, double waveProgress,
        String banner, double bannerAlpha, String error, int unlocked,
        String bossLabel, double bossHealth,
        List<Boolean> activeRows,
        List<CardView> cards, List<DefenderView> defenders, List<EnemyView> enemies,
        List<ProjectileView> projectiles, List<TokenView> tokens,
        List<FloatView> floats, List<EffectView> effects) {

    public record CardView(UnitType type, int cooldownLeft, boolean affordable) {}
    public record DefenderView(UnitType type, int row, int col, int health, int actionAge,
                               int visualRow, int visualFrame, boolean stunned) {}
    public record EnemyView(EnemyType type, int row, double x, int health, boolean biting, int age,
                            boolean flash, double alpha,
                            boolean boss, int phase, BossAction action, int actionAge) {}
    public record ProjectileView(Projectile.Kind kind, int row, double px) {}
    public record TokenView(int x, int y, int ticksLeft) {}
    /** Chu bay len roi mo dan: (x, y) la toa do pixel da tinh san vi tri hien tai. */
    public record FloatView(String text, int x, int y, double alpha) {}
    public record EffectView(Effect.Kind kind, int row, int col, int age, int total) {}

    public static RenderSnapshot empty() {
        return new RenderSnapshot(GameState.MENU, 0, -1, -1, -1, -1, false, 0, null, false, "", 0,
                "", 0, "", 0, "", 0, List.of(true, true, true, true, true),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
