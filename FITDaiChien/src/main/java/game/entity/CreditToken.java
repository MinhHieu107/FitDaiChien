package game.entity;

import game.core.Constants;

public class CreditToken {
    private final Pc owner;
    private final int slot;                       // 0..2: de 3 token cua 1 PC khong chong han len nhau
    private int ticksLeft = Constants.TOKEN_LIFETIME;

    public CreditToken(Pc owner, int slot) { this.owner = owner; this.slot = slot; }

    public Pc owner() { return owner; }
    public int slot() { return slot; }
    public int ticksLeft() { return ticksLeft; }
    public void age() { ticksLeft--; }
    public boolean expired() { return ticksLeft <= 0; }

    /** Toa do pixel (goc tren trai) - model va UI dung chung de hit-test va ve. */
    public int x() { return Constants.GRID_X + owner.col() * Constants.TILE + 2 + slot * 20; }
    public int y() { return Constants.GRID_Y + owner.row() * Constants.TILE + Constants.TILE - Constants.TOKEN_SIZE - 2; }

    public boolean contains(int px, int py) {
        return px >= x() && px < x() + Constants.TOKEN_SIZE && py >= y() && py < y() + Constants.TOKEN_SIZE;
    }
}
