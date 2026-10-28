package game.entity;

public final class UnitFactory {
    private UnitFactory() {}

    /** Loai nao da co code. WEABOO va CHARGER them o buoc 6 (khi do doi thanh luon true). */
    public static boolean isImplemented(UnitType type) {
        return switch (type) {
            case PC, GAMER, YEAR1, YEAR2, YEAR3, YEAR4 -> true;
            case WEABOO, CHARGER -> false;
        };
    }

    public static Defender create(UnitType type, int row, int col) {
        return switch (type) {
            case PC -> new Pc(row, col);
            case GAMER -> new Gamer(row, col);
            case YEAR1, YEAR2, YEAR3, YEAR4 -> new Student(type, row, col);
            default -> throw new UnsupportedOperationException("Chua lam: " + type);
        };
    }
}
