package game.entity;

import game.core.Constants;
import game.core.GameModel;
import java.util.List;

/** 4 don danh cua Weaboo va bang xac suat 40 / 30 / 20 / 10. */
public final class WeabooAttacks {
    private WeabooAttacks() {}

    private abstract static class Base implements WeabooAttack {
        private final String name; private final int weight, anim, row;
        Base(String name, int weight, int anim, int row) { this.name = name; this.weight = weight; this.anim = anim; this.row = row; }
        public String displayName() { return name; }
        public int weight() { return weight; }
        public int animationTicks() { return anim; }
        public int sheetRow() { return row; }
    }

    /** 3 plushie, cach nhau 4 tick, 15 sat thuong moi vien (Weaboo tu giu hang doi vien). */
    public static final WeabooAttack PLUSHIE_BARRAGE = new Base("Plushie Barrage!", 40, 12, 0) {
        public void execute(Weaboo w, GameModel m) { w.startPlushies(m); }
    };

    /** 120 sat thuong cho moi ke dich dang de len o ngay truoc mat (cot + 1). */
    public static final WeabooAttack DAKIMAKURA_SMASH = new Base("Dakimakura Smash!", 30, 12, 1) {
        public void execute(Weaboo w, GameModel m) {
            for (Enemy e : m.enemyList())
                if (!e.isDead() && e.row() == w.row() && e.left() < w.col() + 2 && e.right() > w.col() + 1)
                    e.takeDamage(120);
        }
    };

    /** Khong lam gi, chi tao dang (1 giay). */
    public static final WeabooAttack DRAMATIC_POSE = new Base("...*dramatic pose*", 20, 30, 2) {
        public void execute(Weaboo w, GameModel m) { }
    };

    /** 150 sat thuong ngay lap tuc cho moi ke dich trong hang, ben phai Weaboo. */
    public static final WeabooAttack WIFI_BEAM = new Base("Kamehameha, Wi-Fi Edition!", 10, 15, 3) {
        public void execute(Weaboo w, GameModel m) {
            for (Enemy e : m.enemyList())
                if (!e.isDead() && e.row() == w.row() && e.right() > w.col() + 0.9 && e.left() < Constants.COLS)
                    e.takeDamage(150);
            m.addEffect(Effect.Kind.BEAM, w.row(), w.col(), 15);
        }
    };

    public static final List<WeabooAttack> ALL = List.of(PLUSHIE_BARRAGE, DAKIMAKURA_SMASH, DRAMATIC_POSE, WIFI_BEAM);

    /** roll 0..99 -> don danh: 0-39 plushie, 40-69 smash, 70-89 pose, 90-99 beam. */
    public static WeabooAttack roll(int roll) {
        int acc = 0;
        for (WeabooAttack a : ALL) { acc += a.weight(); if (roll < acc) return a; }
        return ALL.get(ALL.size() - 1);
    }
}
