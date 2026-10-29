package game.entity;

public final class UnitFactory {
    private UnitFactory() {}

    public static boolean isImplemented(UnitType type) { return true; }   // tat ca loai da co code

    public static Defender create(UnitType type, int row, int col) {
        return switch (type) {
            case PC -> new Pc(row, col);
            case GAMER -> new Gamer(row, col);
            case YEAR1, YEAR2, YEAR3, YEAR4 -> new Student(type, row, col);
            case WEABOO -> new Weaboo(row, col);
            case CHARGER -> new BrokenCharger(row, col);
        };
    }
}
