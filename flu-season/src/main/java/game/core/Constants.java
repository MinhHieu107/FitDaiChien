package game.core;

public final class Constants {
    public static final int WIN_W = 900, WIN_H = 600;
    public static final int ROWS = 5, COLS = 9, TILE = 80;
    public static final int GRID_X = 90, GRID_Y = 100;
    public static final int TPS = 30;

    // Sprite: file 160x192, ve ra 64x72 o giua o 80x80
    public static final int SPRITE_W = 64, SPRITE_H = 72;

    // Top bar
    public static final int CARD_X = 10, CARD_Y = 5, CARD_W = 72, CARD_H = 90, CARD_STEP = 80;
    public static final int CREDITS_X = 650, CREDITS_Y = 10, CREDITS_W = 130, CREDITS_H = 40;

    // Kinh te
    public static final int START_CREDITS = 150, MAX_CREDITS = 9999;
    public static final int TOKEN_SIZE = 32, TOKEN_VALUE = 25;
    public static final int TOKEN_LIFETIME = 10 * TPS;
    public static final int MAX_TOKENS_PER_PC = 3;
    public static final int PC_FIRST_TICKS = 5 * TPS;
    public static final int PC_INTERVAL_TICKS = 8 * TPS;

    private Constants() {}
}
