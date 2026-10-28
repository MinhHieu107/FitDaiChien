package game.core;

import game.core.RenderSnapshot.*;
import game.entity.*;
import game.stage.*;
import game.stage.StageConfig.Banner;
import game.stage.StageConfig.Tip;
import java.util.*;

/** Toan bo luat choi. Khong co code Swing. Chi game thread duoc goi cac ham nay. */
public class GameModel {
    public static final int LAST_STAGE = ProgressStore.MAX_STAGE;

    private static final Map<UnitType, String> PLACE_LINES = new EnumMap<>(UnitType.class);
    static {
        PLACE_LINES.put(UnitType.YEAR1,   "I just got here...");
        PLACE_LINES.put(UnitType.YEAR2,   "I know where the printer is.");
        PLACE_LINES.put(UnitType.YEAR3,   "Do not ask me about group projects.");
        PLACE_LINES.put(UnitType.YEAR4,   "I've seen worse deadlines.");
        PLACE_LINES.put(UnitType.GAMER,   "Tanking since 2015. Ez.");
        PLACE_LINES.put(UnitType.PC,      "Loading credits... 99%... 99%...");
        PLACE_LINES.put(UnitType.WEABOO,  "This is just like episode 12!");
        PLACE_LINES.put(UnitType.CHARGER, "Only works if you hold the cable at an angle.");
    }
    private static final String[] TUTORIAL_TEXT = {
            "Welcome to class! Click the PC card, then click a tile to place it.",
            "PCs make credits. Click the credit icon when it appears.",
            "Place a Year 1 student in a row. They throw paper at sick students in that row.",
            "Here they come! Don't let anyone reach the left edge."
    };
    private static final int BANNER_TICKS = 2 * Constants.TPS;

    // ---- tien do / man hinh ----
    private final ProgressStore store;
    private Runnable quitHandler = () -> {};
    private GameState state = GameState.MENU;
    private int unlocked;                 // stage cao nhat da mo (0..3)
    private int currentStage;
    private StageConfig config;
    private String errorMessage = "";

    // ---- trang thai 1 lan choi ----
    private long tick;
    private int mouseX = -1, mouseY = -1, hoverRow = -1, hoverCol = -1;

    private int credits;
    private List<UnitType> cards = List.of();
    private final Map<UnitType, Integer> cooldown = new EnumMap<>(UnitType.class);
    private UnitType selected;

    private final Defender[][] grid = new Defender[Constants.ROWS][Constants.COLS];
    private boolean[] activeRows = new boolean[Constants.ROWS];
    private final List<CreditToken> tokens = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final Set<EnemyType> seenTypes = EnumSet.noneOf(EnemyType.class);

    private List<SpawnEvent> spawns = List.of();
    private int nextSpawn, spawnClock;

    private boolean pcPlaced, tokenCollected, year1Placed;     // cac buoc tutorial
    private String message = "";
    private int messageTicks;
    private String bannerText = "";
    private int bannerTicks;

    public GameModel(ProgressStore store) {
        this.store = store;
        this.unlocked = store.load();
        for (UnitType t : UnitType.values()) cooldown.put(t, 0);
    }

    public void setQuitHandler(Runnable r) { quitHandler = r; }

    // ======================= Dieu huong man hinh =======================

    /** Nap stage tu file va bat dau choi. Loi -> ve MENU kem thong bao, khong crash. */
    public void startStage(int n) {
        StageConfig cfg;
        try {
            cfg = StageLoader.load(n);
        } catch (StageLoadException e) {
            errorMessage = e.getMessage();
            state = GameState.MENU;
            return;
        }
        errorMessage = "";
        currentStage = n;
        config = cfg;
        resetStage();
    }

    private void resetStage() {
        state = GameState.PLAYING;
        tick = 0;
        credits = config.startCredits();
        for (UnitType t : UnitType.values()) cooldown.put(t, 0);
        selected = null;
        for (Defender[] r : grid) Arrays.fill(r, null);
        tokens.clear(); enemies.clear(); projectiles.clear(); seenTypes.clear();
        // Tam thoi bo the cua loai chua lam (Weaboo, Charger). Buoc 6 se xoa dong nay.
        cards = config.cards().stream().filter(UnitFactory::isImplemented).toList();
        activeRows = config.activeRows().clone();
        spawns = config.spawns();
        nextSpawn = 0; spawnClock = 0;
        pcPlaced = tokenCollected = year1Placed = false;
        message = ""; messageTicks = 0;
        bannerText = config.name(); bannerTicks = BANNER_TICKS;
    }

    public void togglePause() {
        if (state == GameState.PLAYING) state = GameState.PAUSED;
        else if (state == GameState.PAUSED) state = GameState.PLAYING;
    }

    // ======================= Lenh tu nguoi choi =======================

    public void setMouse(int x, int y) {
        mouseX = x; mouseY = y;
        int[] t = tileAt(x, y);
        hoverRow = t == null ? -1 : t[0];
        hoverCol = t == null ? -1 : t[1];
    }

    public void cancelSelection() { selected = null; }

    public void handleClick(int x, int y) {
        switch (state) {
            case MENU -> {
                if (Layout.MENU_PLAY.contains(x, y)) { errorMessage = ""; state = GameState.STAGE_SELECT; }
                else if (Layout.MENU_QUIT.contains(x, y)) quitHandler.run();
            }
            case STAGE_SELECT -> {
                for (int i = 0; i <= LAST_STAGE; i++)
                    if (i <= unlocked && Layout.stageButton(i).contains(x, y)) { startStage(i); return; }
                if (Layout.SELECT_BACK.contains(x, y)) state = GameState.MENU;
            }
            case PAUSED -> {
                if (Layout.PRIMARY.contains(x, y)) state = GameState.PLAYING;
                else if (Layout.SECONDARY.contains(x, y)) startStage(currentStage);
                else if (Layout.TERTIARY.contains(x, y)) state = GameState.MENU;
            }
            case WON -> {
                if (Layout.PRIMARY.contains(x, y)) startStage(currentStage + 1);
                else if (Layout.SECONDARY.contains(x, y)) state = GameState.MENU;
            }
            case LOST -> {
                if (Layout.PRIMARY.contains(x, y)) startStage(currentStage);
                else if (Layout.SECONDARY.contains(x, y)) state = GameState.MENU;
            }
            case ENDING -> { if (Layout.PRIMARY.contains(x, y)) state = GameState.MENU; }
            case PLAYING -> clickPlaying(x, y);
        }
    }

    /** Thu tu uu tien: nut UI/the > token > o luoi. (Charger se chen vao giua o buoc sau.) */
    private void clickPlaying(int x, int y) {
        if (Layout.PAUSE_BTN.contains(x, y)) { togglePause(); return; }

        for (int i = 0; i < cards.size(); i++) {
            int cx = Constants.CARD_X + i * Constants.CARD_STEP;
            if (x >= cx && x < cx + Constants.CARD_W && y >= Constants.CARD_Y && y < Constants.CARD_Y + Constants.CARD_H) {
                clickCard(cards.get(i));
                return;
            }
        }
        for (int i = tokens.size() - 1; i >= 0; i--) {
            if (tokens.get(i).contains(x, y)) {
                credits = Math.min(Constants.MAX_CREDITS, credits + Constants.TOKEN_VALUE);
                tokens.remove(i);
                tokenCollected = true;
                return;
            }
        }
        int[] t = tileAt(x, y);
        if (t != null && selected != null && tryPlace(selected, t[0], t[1])) selected = null;
    }

    private void clickCard(UnitType type) {
        if (selected == type) { selected = null; return; }   // bam lai the dang chon = huy
        if (cooldown.get(type) == 0 && credits >= type.cost) selected = type;
    }

    public boolean canPlace(UnitType type, int row, int col) {
        return activeRows[row] && grid[row][col] == null
                && credits >= type.cost && cooldown.get(type) == 0;
    }

    public boolean tryPlace(UnitType type, int row, int col) {
        if (!canPlace(type, row, col)) return false;
        Defender d = UnitFactory.create(type, row, col);     // tao truoc: neu loi thi chua bi tru tien
        credits -= type.cost;
        cooldown.put(type, type.cooldownTicks);
        grid[row][col] = d;
        say(PLACE_LINES.get(type), 60);
        if (type == UnitType.PC) pcPlaced = true;
        if (type == UnitType.YEAR1) year1Placed = true;
        return true;
    }

    private void say(String text, int ticks) { message = text; messageTicks = ticks; }

    // ======================= Duoc entity goi =======================

    public int tokenCount(Pc pc) {
        int n = 0;
        for (CreditToken t : tokens) if (t.owner() == pc) n++;
        return n;
    }

    public void spawnToken(Pc pc) {
        boolean[] used = new boolean[Constants.MAX_TOKENS_PER_PC];
        for (CreditToken t : tokens) if (t.owner() == pc) used[t.slot()] = true;
        for (int s = 0; s < used.length; s++) {
            if (!used[s]) { tokens.add(new CreditToken(pc, s)); return; }
        }
    }

    public void addProjectile(Projectile p) { projectiles.add(p); }

    /** Co ke dich nao o cung hang, ben phai (va trong ban) cua o col khong? */
    public boolean hasEnemyAhead(int row, int col) {
        for (Enemy e : enemies)
            if (e.row() == row && !e.isDead() && e.right() > col + 0.9 && e.left() < Constants.COLS) return true;
        return false;
    }

    /** Defender dang chan ke dich e (hitbox chong nhau). Neu nhieu, lay cai gan e nhat. */
    public Defender blockerOf(Enemy e) {
        Defender best = null;
        for (int c = 0; c < Constants.COLS; c++) {
            Defender d = grid[e.row()][c];
            if (d != null && e.left() < d.right() && e.right() > d.left()) best = d;
        }
        return best;
    }

    // ======================= Moi tick =======================

    public void update() {
        if (state != GameState.PLAYING) return;
        tick++;
        if (messageTicks > 0) messageTicks--;
        if (bannerTicks > 0) bannerTicks--;

        // 2. Sinh ke dich + banner theo gio (tutorial: chi chay sau khi xong 3 buoc dau)
        if (!config.tutorial() || tutorialStep() == 4) {
            spawnClock++;
            for (Banner b : config.banners())
                if (b.tick() == spawnClock) { bannerText = b.text(); bannerTicks = BANNER_TICKS; }
            while (nextSpawn < spawns.size() && spawns.get(nextSpawn).tick() <= spawnClock) {
                SpawnEvent ev = spawns.get(nextSpawn++);
                enemies.add(new Enemy(ev.type(), ev.row()));
                if (seenTypes.add(ev.type())) say(ev.type().line, 90);   // cau thoai lan dau xuat hien
            }
        }

        // 3. Token het han, cooldown the
        for (CreditToken t : tokens) t.age();
        tokens.removeIf(CreditToken::expired);
        cooldown.replaceAll((k, v) -> Math.max(0, v - 1));

        // 4. Defender
        for (Defender[] rowArr : grid)
            for (Defender d : rowArr)
                if (d != null) d.update(this);

        // 5. Dan bay
        for (Projectile p : projectiles) p.update();
        projectiles.removeIf(Projectile::isOut);

        // 6. Ke dich di / can
        for (Enemy e : enemies) e.update(this);

        // 7. Dan trung ke dich gan nhat trong hang
        projectiles.removeIf(p -> {
            Enemy hit = null;
            for (Enemy e : enemies) {
                if (e.row() != p.row() || e.isDead()) continue;
                if (p.left() < e.right() && p.right() > e.left() && (hit == null || e.left() < hit.left())) hit = e;
            }
            if (hit == null) return false;
            hit.takeDamage(p.damage());
            return true;
        });

        // 8. Don xac
        enemies.removeIf(Enemy::isDead);
        for (Defender[] rowArr : grid)
            for (int c = 0; c < rowArr.length; c++)
                if (rowArr[c] != null && rowArr[c].isDead()) rowArr[c] = null;

        // 9. Thang / thua (thua uu tien hon)
        for (Enemy e : enemies) if (e.x() <= 0.0) { state = GameState.LOST; return; }
        if (nextSpawn >= spawns.size() && enemies.isEmpty()) winStage();
    }

    private void winStage() {
        int next = Math.min(LAST_STAGE, currentStage + 1);
        if (next > unlocked) { unlocked = next; store.save(unlocked); }
        state = (currentStage >= LAST_STAGE) ? GameState.ENDING : GameState.WON;
    }

    // ======================= Tutorial & thong diep =======================

    /** 1..4: buoc hien tai. Buoc 4 = dang danh nhau. */
    private int tutorialStep() {
        if (!pcPlaced) return 1;
        if (!tokenCollected) return 2;
        if (!year1Placed) return 3;
        return 4;
    }

    private String currentMessage() {
        if (state == GameState.WON)  return config.tutorial() ? "Nice! Class survived. Next up: Week 1." : "Stage cleared!";
        if (state == GameState.LOST) return "You have been marked absent. See you next semester.";
        if (state == GameState.ENDING) return "Thanks for playing!";
        if (config == null) return "";
        if (messageTicks > 0) return message;
        for (Tip t : config.tips())
            if (spawnClock >= t.startTick() && spawnClock < t.startTick() + t.durationTicks()) return t.text();
        return config.tutorial() ? TUTORIAL_TEXT[tutorialStep() - 1] : "";
    }

    // ======================= Snapshot =======================

    public RenderSnapshot snapshot() {
        List<CardView> cv = new ArrayList<>();
        for (UnitType t : cards) cv.add(new CardView(t, cooldown.get(t), credits >= t.cost));

        List<DefenderView> dv = new ArrayList<>();
        for (Defender[] rowArr : grid)
            for (Defender d : rowArr)
                if (d != null) dv.add(new DefenderView(d.type(), d.row(), d.col(),
                        (int) Math.ceil(d.health()), d.actionAge()));

        List<EnemyView> ev = new ArrayList<>();
        for (Enemy e : enemies)
            ev.add(new EnemyView(e.type(), e.row(), e.x(), (int) Math.ceil(e.health()), e.isBiting(), e.age()));

        List<ProjectileView> pv = new ArrayList<>();
        for (Projectile p : projectiles) pv.add(new ProjectileView(p.kind(), p.row(), p.px()));

        List<TokenView> tv = new ArrayList<>();
        for (CreditToken t : tokens) tv.add(new TokenView(t.x(), t.y(), t.ticksLeft()));

        List<Boolean> rows = new ArrayList<>();
        for (boolean b : activeRows) rows.add(b);

        double wave = spawns.isEmpty() ? 0
                : Math.min(1.0, spawnClock / (double) Math.max(1, spawns.get(spawns.size() - 1).tick()));
        boolean valid = selected != null && hoverRow >= 0 && canPlace(selected, hoverRow, hoverCol);
        double bannerAlpha = bannerTicks <= 0 ? 0 : Math.min(1.0, bannerTicks / 20.0);

        return new RenderSnapshot(state, tick, mouseX, mouseY, hoverRow, hoverCol, valid, credits, selected,
                currentMessage(), wave, bannerText, bannerAlpha, errorMessage, unlocked,
                List.copyOf(rows), List.copyOf(cv), List.copyOf(dv),
                List.copyOf(ev), List.copyOf(pv), List.copyOf(tv));
    }

    // ======================= Tien ich =======================

    /** Pixel -> {row, col}, hoac null neu ngoai luoi. */
    public static int[] tileAt(int x, int y) {
        int c = Math.floorDiv(x - Constants.GRID_X, Constants.TILE);
        int r = Math.floorDiv(y - Constants.GRID_Y, Constants.TILE);
        return (r >= 0 && r < Constants.ROWS && c >= 0 && c < Constants.COLS) ? new int[]{r, c} : null;
    }

    // Dung cho test / debug
    public int credits() { return credits; }
    public void addCredits(int n) { credits = Math.min(Constants.MAX_CREDITS, credits + n); }
    public GameState state() { return state; }
    public int unlocked() { return unlocked; }
    public String error() { return errorMessage; }
    public UnitType selected() { return selected; }
    public Defender defenderAt(int r, int c) { return grid[r][c]; }
    public int enemyCount() { return enemies.size(); }
}
