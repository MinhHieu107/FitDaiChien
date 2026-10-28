package game.core;

/** Vi tri cac nut. Model dung de hit-test, UI dung de ve => chi co 1 nguon su that. */
public final class Layout {
    private Layout() {}

    public record Rect(int x, int y, int w, int h) {
        public boolean contains(int px, int py) { return px >= x && px < x + w && py >= y && py < y + h; }
    }

    public static final Rect PAUSE_BTN = new Rect(800, 10, 90, 40);

    public static final Rect MENU_PLAY = new Rect(350, 340, 200, 50);
    public static final Rect MENU_QUIT = new Rect(350, 405, 200, 50);

    public static Rect stageButton(int i) { return new Rect(260, 150 + i * 72, 380, 56); }
    public static final Rect SELECT_BACK = new Rect(350, 470, 200, 46);

    // Man PAUSED (3 nut) va WON/LOST (2 nut), ENDING (1 nut = PRIMARY)
    public static final Rect PRIMARY   = new Rect(350, 330, 200, 46);
    public static final Rect SECONDARY = new Rect(350, 386, 200, 46);
    public static final Rect TERTIARY  = new Rect(350, 442, 200, 46);
}
