package game.core;

import game.entity.UnitType;
import java.util.List;

/** Ban chup bat bien. Swing chi doc cai nay, khong cham vao model. */
public record RenderSnapshot(
        GameState state, long tick,
        int hoverRow, int hoverCol, boolean hoverValid,
        int credits, UnitType selected,
        List<CardView> cards, List<DefenderView> defenders, List<TokenView> tokens) {

    public record CardView(UnitType type, int cooldownLeft, boolean affordable) {}
    public record DefenderView(UnitType type, int row, int col, int health, int pulseAge) {}
    public record TokenView(int x, int y, int ticksLeft) {}

    public static RenderSnapshot empty() {
        return new RenderSnapshot(GameState.PLAYING, 0, -1, -1, false, 0, null,
                List.of(), List.of(), List.of());
    }
}
