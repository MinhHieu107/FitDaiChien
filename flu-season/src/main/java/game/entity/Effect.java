package game.entity;

/** Hieu ung hinh anh ngan (vu no, tia beam). Khong gay sat thuong: sat thuong da tinh luc kich hoat. */
public class Effect {
    public enum Kind { EXPLOSION, BEAM }

    private final Kind kind;
    private final int row, col, total;
    private int age = 0;

    public Effect(Kind kind, int row, int col, int totalTicks) {
        this.kind = kind; this.row = row; this.col = col; this.total = totalTicks;
    }
    public void tick() { age++; }
    public boolean done() { return age >= total; }
    public Kind kind() { return kind; }
    public int row() { return row; }
    public int col() { return col; }
    public int age() { return age; }
    public int total() { return total; }
}
