package game.entity;

import game.core.GameModel;

/** Strategy: moi don danh cua Weaboo la 1 doi tuong; Weaboo chon 1 cai theo xac suat. */
public interface WeabooAttack {
    String displayName();         // chu bay len tren dau
    int weight();                 // trong so (tong 100)
    int animationTicks();         // so tick phat animation
    int sheetRow();               // hang trong weaboo_moves_atlas.png
    void execute(Weaboo caster, GameModel model);
}
