package game.audio;

import game.core.*;
import game.entity.*;
import game.stage.ProgressStore;
import java.util.*;


public class SoundGameModel extends GameModel {
    // Máu của zombie (máu giảm = bị trúng đạn)
    private final Map<Enemy, Double> mauTruoc = new IdentityHashMap<>();
    // Zombie phát tiếng khi chết
    private final Set<Enemy> daChet = Collections.newSetFromMap(new IdentityHashMap<>());
    // Zombie đang cắn
    private final Set<Enemy> dangCan = Collections.newSetFromMap(new IdentityHashMap<>());

    private boolean daBaoZombie;        // "The Zombies Are Coming"
    private boolean daBaoBoss;          // tiếng boss
    private boolean vuaCoNo;            // tick vụ nổ
    private String bannerTruoc = "";    // chữ trên banner

    public SoundGameModel(ProgressStore store) { super(store); }

    public SoundGameModel(ProgressStore store, Random rng) { super(store, rng); }

    // khi người chơi bấm chuột

    @Override
    public void handleClick(int x, int y) {
        // Ghi nhớ tình hình trước khi click
        GameState stateTruoc = state();
        UnitType theTruoc = selected();
        boolean gianTruoc = digging();
        int soXuTruoc = snapshot().tokens().size();
        int soThe = snapshot().cards().size();

        // Click này có trúng một thẻ ở thanh thẻ phía trên không
        boolean bamVaoThe = false;
        if (y >= Constants.CARD_Y && y < Constants.CARD_Y + Constants.CARD_H) {
            for (int i = 0; i < soThe; i++) {
                int cx = Constants.CARD_X + i * Constants.CARD_STEP;
                if (x >= cx && x < cx + Constants.CARD_W) bamVaoThe = true;
            }
        }

        super.handleClick(x, y);

        // Màn hình vừa đổi -> tiếng click
        if (state() != stateTruoc) { Sfx.play(Sfx.Id.CLICK); return; }
        if (stateTruoc != GameState.PLAYING) return;

        if (bamVaoThe) {
            UnitType theSau = selected();
            if (theSau != null && theSau != theTruoc) Sfx.play(Sfx.Id.CARD_SELECT);
            else if (theTruoc != null && theSau == null) { /* bấm lại thẻ đang chọn = hủy chọn: im lặng */ }
            else Sfx.play(Sfx.Id.BUZZER);
            return;
        }

        // Bấm nút con gián -> tiếng "tách"
        if (digging() != gianTruoc) { Sfx.play(Sfx.Id.CARD_SELECT); return; }

        // Số xu trên màn giảm đi = vừa nhặt được xu -> tiếng "ting"
        if (snapshot().tokens().size() < soXuTruoc) Sfx.play(Sfx.Id.CREDIT);
    }

    @Override
    public boolean tryPlace(UnitType type, int row, int col) {
        boolean thanhCong = super.tryPlace(type, row, col);
        Sfx.play(thanhCong ? Sfx.Id.PLACE : Sfx.Id.BUZZER);
        return thanhCong;
    }

    @Override
    public boolean dig(int row, int col) {
        boolean thanhCong = super.dig(row, col);
        Sfx.play(thanhCong ? Sfx.Id.DIG : Sfx.Id.BUZZER);
        return thanhCong;
    }

    @Override
    public void advanceIntro() {
        boolean dangIntro = state() == GameState.INTRO;
        super.advanceIntro();
        if (dangIntro) Sfx.play(Sfx.Id.CLICK);
    }

    @Override
    public void spawnToken(Pc pc) {
        int truoc = tokenCount(pc);
        super.spawnToken(pc);
        if (tokenCount(pc) > truoc) Sfx.play(Sfx.Id.TOKEN_SPAWN);
    }

    @Override
    public void addProjectile(Projectile p) {
        super.addProjectile(p);
        Sfx.play(Sfx.Id.THROW);
    }

    @Override
    public void addEffect(Effect.Kind kind, int row, int col, int ticks) {
        super.addEffect(kind, row, col, ticks);
        if (kind == Effect.Kind.EXPLOSION || kind == Effect.Kind.BREACH) {
            Sfx.play(Sfx.Id.EXPLOSION);
            vuaCoNo = true;
        }
    }

    @Override
    public void startStage(int n) {
        super.startStage(n);
        mauTruoc.clear();
        daChet.clear();
        dangCan.clear();
        daBaoZombie = false;
        daBaoBoss = false;
        vuaCoNo = false;
        bannerTruoc = snapshot().banner();
    }

    @Override
    public void update() {
        GameState truoc = state();
        vuaCoNo = false;

        super.update();

        if (truoc == GameState.INTRO) { phatTiengGoChu(); return; }

        // Đang ở menu / tạm dừng / màn thắng thua không có gì xảy ra
        if (truoc != GameState.PLAYING) return;

        phatTiengZombie();
        phatTiengBanner();

        // Vừa thua -> phát nhạc thua.
        if (state() == GameState.LOST) Sfx.play(Sfx.Id.LOSE);
    }

    private void phatTiengZombie() {
        List<Enemy> ds = enemyList();

        // zombie đầu tiên của màn chơi xuất hiện -> "The Zombies Are Coming"
        if (!daBaoZombie && !ds.isEmpty()) {
            daBaoZombie = true;
            Sfx.play(Sfx.Id.ZOMBIES_COMING);
        }

        // Boss vừa xuất hiện -> tiếng gầm
        if (!daBaoBoss && boss() != null) {
            daBaoBoss = true;
            Sfx.play(Sfx.Id.BOSS);
        }

        boolean trungDan = false;
        Set<Enemy> hienCo = Collections.newSetFromMap(new IdentityHashMap<>());

        for (Enemy e : ds) {
            hienCo.add(e);

            // Máu giảm -> vừa bị đạn trúng
            Double mauCu = mauTruoc.put(e, e.health());
            if (mauCu != null && e.health() < mauCu) trungDan = true;

            // Vừa chết
            if (e.isDead() && daChet.add(e)) Sfx.play(Sfx.Id.DEATH);

            // Cắn nhân vật
            if (e.isBiting()) {
                if (dangCan.add(e)) Sfx.play(Sfx.Id.BRAINS);
                if (e.age() % 20 == 0) Sfx.play(Sfx.Id.CHOMP);
            } else {
                dangCan.remove(e);
            }
        }

        // Có zombie bị trúng đạn -> tiếng trúng đạn
        if (trungDan && !vuaCoNo) Sfx.play(Sfx.Id.HIT);

        mauTruoc.keySet().retainAll(hienCo);
        daChet.retainAll(hienCo);
        dangCan.retainAll(hienCo);
    }

    private void phatTiengGoChu() {
        RenderSnapshot s = snapshot();
        if (!Intro.complete(s.introPage(), s.introTicks()) && s.introTicks() % 2 == 0) Sfx.play(Sfx.Id.TYPE);
    }

    private void phatTiengBanner() {
        RenderSnapshot s = snapshot();
        String chu = s.banner();
        if (s.bannerAlpha() > 0 && !chu.equals(bannerTruoc)) Sfx.play(Sfx.Id.ALERT);
        bannerTruoc = chu;
    }
}
