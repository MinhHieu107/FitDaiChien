package game.entity;

import game.core.Constants;
import game.core.GameModel;

public class Pc extends Defender {
    private int timer = 0;
    private boolean first = true;
    private int pulseAge = Integer.MAX_VALUE;

    public Pc(int row, int col) { super(UnitType.PC, row, col); }

    @Override public void update(GameModel model) {
        if (pulseAge < 1000) pulseAge++;
        timer++;
        int target = first ? Constants.PC_FIRST_TICKS : Constants.PC_INTERVAL_TICKS;
        // Dang co du 3 token chua nhat thi cho (timer van giu o muc san sang)
        if (timer >= target && model.tokenCount(this) < Constants.MAX_TOKENS_PER_PC) {
            model.spawnToken(this);
            timer = 0;
            first = false;
            pulseAge = 0;
        }
    }

    @Override public int pulseAge() { return pulseAge; }
}
