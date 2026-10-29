package game.entity;

/** Phase cua boss theo % mau. Chi di tien, khong bao gio quay lai. */
public enum BossPhase {
    LECTURE("Lecture"),          // > 60%
    POP_QUIZ("Pop Quiz"),        // 60% xuong > 30%
    FINALS_RAGE("Finals Rage");  // <= 30%

    public final String label;
    BossPhase(String label) { this.label = label; }

    /** Phase muc tieu ung voi ti le mau con lai (0..1). */
    public static BossPhase forHealth(double fraction) {
        if (fraction > 0.60) return LECTURE;
        if (fraction > 0.30) return POP_QUIZ;
        return FINALS_RAGE;
    }
}
