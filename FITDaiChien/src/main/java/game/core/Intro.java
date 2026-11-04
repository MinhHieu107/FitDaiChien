package game.core;

/** Loi gioi thieu truoc stage 0: tung trang, chu hien dan nhu may danh chu. Dung chung cho model (nhip) va UI (ve). */
public final class Intro {
    private Intro() {}

    public static final String[] PAGES = {
            "The semester at the Faculty of Information Technology was going smoothly—until the Deadline Professor "
                    + "rolled into the lecture hall with a towering stack of assignments.",
            "“Good news,” he announced. “Your project is due yesterday!”",
            "A wave of panic swept through campus. The deadline hit so hard, it transform the already weaken student "
                    + "into a purple monster that seeks to germinate the disease to everyone so no one would pass the subject.",
            "Place students along the campus path, earn study points, and stop the Deadline Monsters before they reach "
                    + "the server room. Your students have different specialties, so choose your team carefully."
    };

    /** Toc do chu: 1.5 ky tu moi tick (45 ky tu/giay). */
    public static int visibleChars(int page, int ticks) {
        return Math.min(PAGES[page].length(), ticks * 3 / 2);
    }

    /** So tick toi thieu de hien het trang. */
    public static int ticksToComplete(int page) { return (PAGES[page].length() * 2 + 2) / 3; }

    public static boolean complete(int page, int ticks) { return visibleChars(page, ticks) >= PAGES[page].length(); }
}
