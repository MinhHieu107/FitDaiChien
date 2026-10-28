package game.stage;

import game.entity.EnemyType;
import java.util.List;

/** TAM THOI: stage viet cung. Buoc 4 se thay bang StageLoader doc file. */
public final class Stages {
    private Stages() {}

    /** Stage 0 - Orientation Day. Danh sach phai sap xep theo thoi gian. */
    public static List<SpawnEvent> tutorialSpawns() {
        return List.of(
                SpawnEvent.of(0,  EnemyType.SICK, 3),
                SpawnEvent.of(12, EnemyType.SICK, 3),
                SpawnEvent.of(24, EnemyType.SICK, 2));
    }
}
