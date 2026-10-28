package game.stage;

import game.core.Constants;
import game.entity.EnemyType;
import game.entity.UnitType;
import game.stage.StageConfig.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Doc stage tu file text dang key=value (xem GAME_DESIGN_README muc 14).
 * Bat ky loi nao (thieu file, dong sai, id la) deu nem StageLoadException kem so dong,
 * de game hien thong bao va quay ve menu thay vi crash.
 */
public final class StageLoader {
    private StageLoader() {}

    /** Doc /stages/stage{n}.txt trong classpath (resources). */
    public static StageConfig load(int stageIndex) throws StageLoadException {
        String file = "stage" + stageIndex + ".txt";
        try (InputStream in = StageLoader.class.getResourceAsStream("/stages/" + file)) {
            if (in == null) throw new StageLoadException("Missing stage file: " + file);
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            List<String> lines = new ArrayList<>();
            for (String line; (line = r.readLine()) != null; ) lines.add(line);
            return parse(lines, file);
        } catch (IOException e) {
            throw new StageLoadException("Cannot read " + file + ": " + e.getMessage(), e);
        }
    }

    /** Tach rieng ham parse de test khong can file. */
    public static StageConfig parse(List<String> lines, String source) throws StageLoadException {
        String name = null;
        boolean[] rows = {true, true, true, true, true};
        int credits = 150;
        List<UnitType> cards = new ArrayList<>(), newCards = new ArrayList<>();
        List<Tip> tips = new ArrayList<>();
        List<Banner> banners = new ArrayList<>();
        List<SpawnEvent> spawns = new ArrayList<>();
        BossSpawn boss = null;
        boolean tutorial = false;

        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i).strip();
            if (raw.isEmpty() || raw.startsWith("#")) continue;
            String where = source + " line " + (i + 1);
            int eq = raw.indexOf('=');
            if (eq < 0) throw new StageLoadException(where + ": expected key=value, got '" + raw + "'");
            String key = raw.substring(0, eq).strip();
            String val = raw.substring(eq + 1).strip();

            switch (key) {
                case "name" -> name = val;
                case "activeRows" -> {
                    rows = new boolean[Constants.ROWS];
                    for (String p : val.split(",")) rows[row1(p, where)] = true;
                }
                case "startCredits" -> credits = intOf(val, where, 0, Constants.MAX_CREDITS);
                case "cards" -> cards = unitList(val, where);
                case "newCards" -> newCards = unitList(val, where);
                case "tip" -> {
                    String[] p = val.split(",", 3);
                    if (p.length < 3) throw new StageLoadException(where + ": tip=start,duration,text");
                    tips.add(new Tip(sec(p[0], where), sec(p[1], where), p[2].strip()));
                }
                case "banner" -> {
                    String[] p = val.split(",", 2);
                    if (p.length < 2) throw new StageLoadException(where + ": banner=seconds,text");
                    banners.add(new Banner(sec(p[0], where), p[1].strip()));
                }
                case "spawn" -> {
                    String[] p = parts(val, 3, where, "spawn=seconds,TYPE,row");
                    EnemyType type;
                    try { type = EnemyType.valueOf(p[1].strip()); }
                    catch (IllegalArgumentException e) {
                        throw new StageLoadException(where + ": unknown enemy type '" + p[1].strip() + "'");
                    }
                    spawns.add(new SpawnEvent(sec(p[0], where), type, row1(p[2], where)));
                }
                case "boss" -> {
                    String[] p = parts(val, 3, where, "boss=seconds,PROF,row");
                    if (!p[1].strip().equals("PROF"))
                        throw new StageLoadException(where + ": unknown boss '" + p[1].strip() + "'");
                    boss = new BossSpawn(sec(p[0], where), row1(p[2], where));
                }
                case "tutorial" -> tutorial = Boolean.parseBoolean(val);
                default -> throw new StageLoadException(where + ": unknown key '" + key + "'");
            }
        }

        if (name == null) throw new StageLoadException(source + ": missing 'name'");
        if (cards.isEmpty()) throw new StageLoadException(source + ": missing 'cards'");
        if (spawns.isEmpty() && boss == null) throw new StageLoadException(source + ": stage has no enemies");

        spawns.sort(Comparator.comparingInt(SpawnEvent::tick));     // sort on dinh: giu thu tu trong file
        return new StageConfig(name, rows, credits, List.copyOf(cards), List.copyOf(newCards),
                List.copyOf(tips), List.copyOf(banners), List.copyOf(spawns), boss, tutorial);
    }

    // ---------- tien ich parse ----------

    private static String[] parts(String val, int n, String where, String usage) throws StageLoadException {
        String[] p = val.split(",");
        if (p.length != n) throw new StageLoadException(where + ": expected " + usage);
        return p;
    }

    private static int intOf(String s, String where, int min, int max) throws StageLoadException {
        try {
            int v = Integer.parseInt(s.strip());
            if (v < min || v > max) throw new StageLoadException(where + ": " + v + " out of range " + min + ".." + max);
            return v;
        } catch (NumberFormatException e) {
            throw new StageLoadException(where + ": '" + s.strip() + "' is not a number");
        }
    }

    /** Giay -> tick. */
    private static int sec(String s, String where) throws StageLoadException {
        return intOf(s, where, 0, 100_000) * Constants.TPS;
    }

    /** Hang 1..5 trong file -> index 0..4. */
    private static int row1(String s, String where) throws StageLoadException {
        return intOf(s, where, 1, Constants.ROWS) - 1;
    }

    private static List<UnitType> unitList(String val, String where) throws StageLoadException {
        List<UnitType> out = new ArrayList<>();
        for (String p : val.split(",")) {
            try { out.add(UnitType.valueOf(p.strip())); }
            catch (IllegalArgumentException e) {
                throw new StageLoadException(where + ": unknown card id '" + p.strip() + "'");
            }
        }
        return out;
    }
}
