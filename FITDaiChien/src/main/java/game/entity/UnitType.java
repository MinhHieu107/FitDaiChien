package game.entity;

import game.core.Constants;

/** Bang chi so cua defender + vi tri the trong unit_cards.png (4 cot x 2 hang). */
public enum UnitType {
    //      cost health cooldown(s) cardCol cardRow sprite
    PC     ( 50,  300,  5, 0, 0, "pc_placed"),
    YEAR1  (100,  300,  5, 1, 0, "year1_student_placed"),
    GAMER  ( 50, 1500, 15, 2, 0, "gamer_placed"),
    CHARGER( 75,  100, 20, 3, 0, "broken_charger_placed"),
    WEABOO (150,  300, 10, 0, 1, "weaboo_placed"),
    YEAR2  (150,  300,  7, 1, 1, "year2_student_placed"),
    YEAR3  (250,  400, 10, 2, 1, "year3_student_placed"),
    YEAR4  (350,  500, 15, 3, 1, "year4_student_placed");

    public final int cost, health, cooldownTicks, cardCol, cardRow;
    public final String sprite;

    UnitType(int cost, int health, int cooldownSec, int cardCol, int cardRow, String sprite) {
        this.cost = cost;
        this.health = health;
        this.cooldownTicks = cooldownSec * Constants.TPS;
        this.cardCol = cardCol;
        this.cardRow = cardRow;
        this.sprite = sprite;
    }
}
