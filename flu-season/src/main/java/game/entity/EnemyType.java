package game.entity;

public enum EnemyType {
    //     health speed(o/giay) bite/giay  walkSheet                              biteRow
    SICK   (200, 0.25, 20, "animation_sheets/walk_sick_student.png",         0, "*cough* Is attendance mandatory?"),
    TISSUE (120, 0.45, 20, "animation_sheets/walk_sick_student_tissue.png",  1, "*sniff* Wait for me!"),
    BLANKET(500, 0.18, 20, "animation_sheets/walk_sick_student_blanket.png", 2, "Five more minutes...");

    public final int health;
    public final double speed;
    public final int bitePerSecond;
    public final String walkSheet;
    public final int biteRow;
    public final String line;       // cau thoai hien lan dau loai nay xuat hien trong stage

    EnemyType(int health, double speed, int bitePerSecond, String walkSheet, int biteRow, String line) {
        this.health = health;
        this.speed = speed;
        this.bitePerSecond = bitePerSecond;
        this.walkSheet = walkSheet;
        this.biteRow = biteRow;
        this.line = line;
    }
}
