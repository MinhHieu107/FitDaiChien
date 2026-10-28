package game.entity;

public final class UnitFactory {
    private UnitFactory() {}

    public static Defender create(UnitType type, int row, int col) {
        return switch (type) {
            case PC -> new Pc(row, col);
            case YEAR1, YEAR2, YEAR3, YEAR4 -> new Student(type, row, col);
            default -> throw new UnsupportedOperationException("Chua lam: " + type);
        };
    }
}
