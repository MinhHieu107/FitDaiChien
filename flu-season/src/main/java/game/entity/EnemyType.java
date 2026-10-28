package game.entity;

public enum EnemyType {
    //     health speed(o/giay) bite/giay  walkSheet                              biteRow
    SICK   (200, 0.25, 20, "animation_sheets/walk_sick_student.png",         0),
    TISSUE (120, 0.45, 20, "animation_sheets/walk_sick_student_tissue.png",  1),
    BLANKET(500, 0.18, 20, "animation_sheets/walk_sick_student_blanket.png", 2);

    public final int health;
    public final double speed;
    public final int bitePerSecond;
    public final String walkSheet;
    public final int biteRow;

    EnemyType(int health, double speed, int bitePerSecond, String walkSheet, int biteRow) {
        this.health = health;
        this.speed = speed;
        this.bitePerSecond = bitePerSecond;
        this.walkSheet = walkSheet;
        this.biteRow = biteRow;
    }
}
