package game.core;

/** Vi tri cac nut. Model dung de hit-test, UI dung de ve => chi co 1 nguon su that. */
public final class Layout {
    private Layout() {}

    public record Rect(int x, int y, int w, int h) {
        public boolean contains(int px, int py) { return px >= x && px < x + w && py >= y && py < y + h; }
    }

    public static final Rect PAUSE_BTN = new Rect(820, 10, 70, 40);
    /** Nut con gian (go quan): ngay canh o tien tren thanh tren (8 the ket thuc o x=642). */
    public static final Rect DIG_BTN = new Rect(646, 5, 62, 90);

    public static final Rect MENU_PLAY = new Rect(350, 325, 200, 50);
    public static final Rect MENU_HELP = new Rect(350, 389, 200, 50);
    public static final Rect MENU_QUIT = new Rect(350, 453, 200, 50);
    public static final Rect HELP_BACK = new Rect(350, 538, 200, 46);

    public static Rect stageButton(int i) { return new Rect(260, 150 + i * 72, 380, 56); }
    public static final Rect SELECT_BACK = new Rect(350, 470, 200, 46);

    // Man PAUSED (3 nut) va WON/LOST (2 nut), ENDING (1 nut = PRIMARY)
    public static final Rect PRIMARY   = new Rect(350, 330, 200, 46);
    public static final Rect SECONDARY = new Rect(350, 386, 200, 46);
    public static final Rect TERTIARY  = new Rect(350, 442, 200, 46);
}
