package game.entity;

/** Hanh dong dang dien cua boss (chi de chon anh; sat thuong/hieu ung do Boss va GameModel xu ly). */
public enum BossAction {
    CHALK     (12,  "prof_chalk_throw_strip.png"),
    SUMMON    (12,  "prof_summon_partner_strip.png"),
    QUIZ      (12,  "prof_pop_quiz_strip.png"),
    MOVED_UP  (150, "prof_deadline_moved_up_strip.png"),     // giu pose suot 5 giay buff
    ATTENDANCE(120, "prof_attendance_check_strip.png"),      // giu pose suot 4 giay PC bi dung
    DEFEAT    (-1,  "prof_deadline_defeat_strip.png");       // giu den het man

    public final int ticks;            // -1 = giu mai
    public final String sprite;
    BossAction(int ticks, String sprite) { this.ticks = ticks; this.sprite = sprite; }
}
