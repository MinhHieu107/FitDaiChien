package game.stage;

import game.core.Constants;
import game.entity.EnemyType;

/** Mot lan sinh ke dich: tick = thoi diem (tinh tu luc stage bat dau), row tinh tu 0. */
public record SpawnEvent(int tick, EnemyType type, int row) {
    /** Tien loi: giay + hang 1..5 (dung cho file stage). */
    public static SpawnEvent of(int seconds, EnemyType type, int row1Based) {
        return new SpawnEvent(seconds * Constants.TPS, type, row1Based - 1);
    }
}
