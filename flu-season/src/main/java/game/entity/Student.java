package game.entity;

import game.core.GameModel;

/** Year 1..4. Chi so doc tu UnitType; buoc 3 se them logic ban. */
public class Student extends Defender {
    public Student(UnitType type, int row, int col) { super(type, row, col); }

    @Override public void update(GameModel model) {
        // TODO buoc 3: ban paper ball
    }
}
