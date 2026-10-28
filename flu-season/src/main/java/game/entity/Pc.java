package game.entity;

import game.core.Constants;
import game.core.GameModel;

public class Pc extends Defender {
    private int timer = 0;
    private boolean first = true;
    private int actionAge = Integer.MAX_VALUE;

    public Pc(int row, int col) { super(UnitType.PC, row, col); }

    @Override public void update(GameModel model) {
        if (actionAge < 1000) actionAge++;
        timer++;
        int target = first ? Constants.PC_FIRST_TICKS : Constants.PC_INTERVAL_TICKS;
        if (timer >= target && model.tokenCount(this) < Constants.MAX_TOKENS_PER_PC) {
            model.spawnToken(this);
            timer = 0;
            first = false;
            actionAge = 0;
        }
    }

    @Override public int actionAge() { return actionAge; }
}
