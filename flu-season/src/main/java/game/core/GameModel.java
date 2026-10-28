package game.core;

import game.core.RenderSnapshot.*;
import game.entity.*;
import game.stage.*;
import java.util.*;

/** Toan bo luat choi. Khong co code Swing. Chi game thread duoc goi cac ham nay. */
public class GameModel {
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
    private static final int RESTART_DELAY_TICKS = 45;     // tranh bam nham ngay khi vua thang/thua

    private GameState state;
    private long tick, stateTicks;
    private int hoverRow, hoverCol;

    private int credits;
    private final List<UnitType> cards = List.of(UnitType.PC, UnitType.YEAR1); // tam thoi, sau se doc tu stage
    private final Map<UnitType, Integer> cooldown = new EnumMap<>(UnitType.class);
    private UnitType selected;

    private final Defender[][] grid = new Defender[Constants.ROWS][Constants.COLS];
    private boolean[] activeRows;
    private final List<CreditToken> tokens = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();

    private List<SpawnEvent> spawns;
    private int nextSpawn, spawnClock;

    private boolean tutorial, pcPlaced, tokenCollected, year1Placed;
    private String message;
    private int messageTicks;

    public GameModel() { restart(); }

    /** Dat lai toan bo stage (cung dung cho nut Retry/Restart sau nay). */
    public void restart() {
        state = GameState.PLAYING;
        tick = 0; stateTicks = 0;
        hoverRow = hoverCol = -1;
        credits = Constants.START_CREDITS;
        for (UnitType t : UnitType.values()) cooldown.put(t, 0);
        selected = null;
        for (Defender[] r : grid) Arrays.fill(r, null);
        tokens.clear(); enemies.clear(); projectiles.clear();
        activeRows = new boolean[]{false, true, true, true, false};   // tutorial: chi hang 2,3,4
        spawns = Stages.tutorialSpawns();
        nextSpawn = 0; spawnClock = 0;
        tutorial = true; pcPlaced = tokenCollected = year1Placed = false;
        message = ""; messageTicks = 0;
    }

    // ---------- Lenh tu nguoi choi (duoc goi boi Command) ----------

    public void setHover(int r, int c) { hoverRow = r; hoverCol = c; }
    public void cancelSelection() { selected = null; }

    public void togglePause() {
        if (state == GameState.PLAYING) state = GameState.PAUSED;
        else if (state == GameState.PAUSED) state = GameState.PLAYING;
    }

    /** Thu tu uu tien: the > token > o luoi. (Charger se chen vao giua o buoc sau.) */
    public void handleClick(int x, int y) {
        if (state == GameState.WON || state == GameState.LOST) {
            if (stateTicks >= RESTART_DELAY_TICKS) restart();
            return;
        }
        if (state != GameState.PLAYING) return;

        // 1. The bai
        for (int i = 0; i < cards.size(); i++) {
            int cx = Constants.CARD_X + i * Constants.CARD_STEP;
            if (x >= cx && x < cx + Constants.CARD_W && y >= Constants.CARD_Y && y < Constants.CARD_Y + Constants.CARD_H) {
                clickCard(cards.get(i));
                return;
            }
        }
        // 2. Credit token (duyet nguoc: cai ve sau nam tren)
        for (int i = tokens.size() - 1; i >= 0; i--) {
            if (tokens.get(i).contains(x, y)) {
                credits = Math.min(Constants.MAX_CREDITS, credits + Constants.TOKEN_VALUE);
                tokens.remove(i);
                tokenCollected = true;
                return;
            }
        }
        // 3. O luoi
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
        credits -= type.cost;
        cooldown.put(type, type.cooldownTicks);
        grid[row][col] = UnitFactory.create(type, row, col);
        say(PLACE_LINES.get(type), 60);
        if (type == UnitType.PC) pcPlaced = true;
        if (type == UnitType.YEAR1) year1Placed = true;
        return true;
    }

    private void say(String text, int ticks) { message = text; messageTicks = ticks; }

    // ---------- Duoc entity goi ----------

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
            if (d != null && e.left() < d.right() && e.right() > d.left()) best = d;   // c lon hon = gan ke dich hon
        }
        return best;
    }

    // ---------- Moi tick (thu tu theo muc 5 cua design) ----------

    public void update() {
        if (state != GameState.PLAYING) { stateTicks++; return; }
        tick++;
        if (messageTicks > 0) messageTicks--;

        // 2. Sinh ke dich (tutorial: chi sau khi xong 3 buoc dau)
        if (!tutorial || tutorialStep() == 4) {
            spawnClock++;
            while (nextSpawn < spawns.size() && spawns.get(nextSpawn).tick() <= spawnClock) {
                SpawnEvent ev = spawns.get(nextSpawn++);
                enemies.add(new Enemy(ev.type(), ev.row()));
            }
        }

        // 3. Token het han, cooldown the
        for (CreditToken t : tokens) t.age();
        tokens.removeIf(CreditToken::expired);
        cooldown.replaceAll((k, v) -> Math.max(0, v - 1));

        // 4. Defender (PC sinh token, Student ban)
        for (Defender[] rowArr : grid)
            for (Defender d : rowArr)
                if (d != null) d.update(this);

        // 5. Dan bay
        for (Projectile p : projectiles) p.update();
        projectiles.removeIf(Projectile::isOut);

        // 6. Ke dich di / can
        for (Enemy e : enemies) e.update(this);

        // 7. Dan trung ke dich: trung ke dich gan nhat (x nho nhat) trong hang
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
        for (Enemy e : enemies) if (e.x() <= 0.0) { state = GameState.LOST; stateTicks = 0; return; }
        boolean allSpawned = nextSpawn >= spawns.size();
        if (allSpawned && enemies.isEmpty()) { state = GameState.WON; stateTicks = 0; }
    }

    // ---------- Tutorial ----------

    /** 1..4: buoc hien tai. Buoc 4 = dang danh nhau. */
    private int tutorialStep() {
        if (!pcPlaced) return 1;
        if (!tokenCollected) return 2;
        if (!year1Placed) return 3;
        return 4;
    }

    private String currentMessage() {
        if (state == GameState.WON) return tutorial ? "Nice! Class survived. Next up: Week 1." : "Stage cleared!";
        if (state == GameState.LOST) return "You have been marked absent. See you next semester.";
        if (messageTicks > 0) return message;
        return tutorial ? TUTORIAL_TEXT[tutorialStep() - 1] : "";
    }

    // ---------- Snapshot ----------

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
                : Math.min(1.0, spawnClock / (double) spawns.get(spawns.size() - 1).tick());
        boolean valid = selected != null && hoverRow >= 0 && canPlace(selected, hoverRow, hoverCol);

        return new RenderSnapshot(state, tick, hoverRow, hoverCol, valid, credits, selected,
                currentMessage(), wave, List.copyOf(rows), List.copyOf(cv), List.copyOf(dv),
                List.copyOf(ev), List.copyOf(pv), List.copyOf(tv));
    }

    // ---------- Tien ich ----------

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
    public UnitType selected() { return selected; }
    public Defender defenderAt(int r, int c) { return grid[r][c]; }
    public int enemyCount() { return enemies.size(); }
}
