package game.core;
public record RenderSnapshot(GameState state, long tick, int hoverRow, int hoverCol) {}