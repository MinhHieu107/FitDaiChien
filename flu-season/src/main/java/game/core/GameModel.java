package game.core;

import game.core.RenderSnapshot.*;
import game.entity.*;
import java.util.*;

/** Toan bo luat choi. Khong co code Swing. Chi game thread duoc goi cac ham nay. */
public class GameModel {
    private GameState state = GameState.PLAYING;
    private long tick = 0;
    private int hoverRow = -1, hoverCol = -1;

    private int credits = Constants.START_CREDITS;
    private final List<UnitType> cards = List.of(UnitType.PC, UnitType.YEAR1); // tam thoi, sau se doc tu stage
    private final Map<UnitType, Integer> cooldown = new EnumMap<>(UnitType.class);
    private UnitType selected = null;

    private final Defender[][] grid = new Defender[Constants.ROWS][Constants.COLS];
    private final boolean[] activeRows = {true, true, true, true, true};
    private final List<CreditToken> tokens = new ArrayList<>();

    public GameModel() { for (UnitType t : cards) cooldown.put(t, 0); }

    // ---------- Lenh tu nguoi choi (duoc goi boi Command) ----------

    public void setHover(int r, int c) { hoverRow = r; hoverCol = c; }
    public void cancelSelection() { selected = null; }
    public void setState(GameState s) { state = s; }

    /** Thu tu uu tien: the > token > o luoi. (Charger se chen vao giua o buoc sau.) */
    public void handleClick(int x, int y) {
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
        return true;
    }

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

    // ---------- Moi tick ----------

    public void update() {
        if (state != GameState.PLAYING) return;
        tick++;

        // token het han
        for (CreditToken t : tokens) t.age();
        tokens.removeIf(CreditToken::expired);

        // cooldown the bai
        cooldown.replaceAll((k, v) -> Math.max(0, v - 1));

        // defender
        for (Defender[] rowArr : grid)
            for (Defender d : rowArr)
                if (d != null) d.update(this);
    }

    // ---------- Snapshot ----------

    public RenderSnapshot snapshot() {
        List<CardView> cv = new ArrayList<>();
        for (UnitType t : cards) cv.add(new CardView(t, cooldown.get(t), credits >= t.cost));

        List<DefenderView> dv = new ArrayList<>();
        for (Defender[] rowArr : grid)
            for (Defender d : rowArr)
                if (d != null) dv.add(new DefenderView(d.type(), d.row(), d.col(), d.health(), d.pulseAge()));

        List<TokenView> tv = new ArrayList<>();
        for (CreditToken t : tokens) tv.add(new TokenView(t.x(), t.y(), t.ticksLeft()));

        boolean valid = selected != null && hoverRow >= 0 && canPlace(selected, hoverRow, hoverCol);
        return new RenderSnapshot(state, tick, hoverRow, hoverCol, valid, credits, selected,
                List.copyOf(cv), List.copyOf(dv), List.copyOf(tv));
    }

    // ---------- Tien ich ----------

    /** Pixel -> {row, col}, hoac null neu ngoai luoi. */
    public static int[] tileAt(int x, int y) {
        int c = Math.floorDiv(x - Constants.GRID_X, Constants.TILE);
        int r = Math.floorDiv(y - Constants.GRID_Y, Constants.TILE);
        return (r >= 0 && r < Constants.ROWS && c >= 0 && c < Constants.COLS) ? new int[]{r, c} : null;
    }

    // chi dung cho test
    public int credits() { return credits; }
    public Defender defenderAt(int r, int c) { return grid[r][c]; }
    public UnitType selected() { return selected; }
}
