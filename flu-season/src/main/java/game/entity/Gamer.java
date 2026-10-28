package game.entity;

import game.core.GameModel;

/** Tank: khong tan cong, chi chiu don. Sprite doi sang "tired" khi mau < 50% (xu ly o GamePanel). */
public class Gamer extends Defender {
    public Gamer(int row, int col) { super(UnitType.GAMER, row, col); }
    @Override public void update(GameModel model) { /* khong lam gi */ }
}
