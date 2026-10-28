package game.stage;

import game.entity.UnitType;
import java.util.List;

/** Du lieu 1 stage, doc tu file. Bat bien. activeRows co 5 phan tu (hang 0..4). */
public record StageConfig(
        String name,
        boolean[] activeRows,
        int startCredits,
        List<UnitType> cards,
        List<UnitType> newCards,
        List<Tip> tips,
        List<Banner> banners,
        List<SpawnEvent> spawns,
        BossSpawn boss,            // null neu stage khong co boss
        boolean tutorial) {

    /** Dong chu o thanh duoi, tu startTick keo dai durationTicks. */
    public record Tip(int startTick, int durationTicks, String text) {}
    /** Chu lon giua man hinh tai thoi diem tick. */
    public record Banner(int tick, String text) {}
    public record BossSpawn(int tick, int row) {}
}
