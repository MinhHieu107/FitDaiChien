# Complete Sprite Guide

This guide maps every sprite in this folder to a game object, screen state, animation row, and game event. It follows `GAME_DESIGN_README.md`. Images provide visuals only: Java still owns movement, health, targeting, damage, unlocks, cooldowns, and timers.

## 1. Install and draw the assets

1. Unzip the sprite pack.
2. Copy the contents of its `game_assets/` folder into `src/main/resources/sprites/` in the Java project. Keep the `animation_sheets/` and `placed/` subfolders.
3. Load each PNG once with `ImageIO.read(...)` and cache the `BufferedImage`.
4. Draw with nearest-neighbor interpolation. Normal character files are 160 × 192 source pixels. The design says normal defenders are drawn at 64 × 72 screen pixels, centered in an 80 × 80 tile. Draw Prof. Deadline larger, about 96 × 112 screen pixels. The tile art files are 64 × 64; scale them to the 80 × 80 board tiles.

```java
static void drawFrame(Graphics2D g, BufferedImage sheet,
                      int frame, int row, int frameW, int frameH,
                      int x, int y, int drawW, int drawH) {
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                       RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    int sx = frame * frameW;
    int sy = row * frameH;
    g.drawImage(sheet, x, y, x + drawW, y + drawH,
                sx, sy, sx + frameW, sy + frameH, null);
}
```

Frames and rows are zero-based. For a one-row strip use `row = 0`. For a separate 4-frame strip, use `frame = 0..3`. For an atlas, use the row and column listed below. Keep the game entity's x/y in the model; changing animation frames must not move the entity or apply damage.

## 2. Menu cards, placement, and board feedback

### Card atlas

`unit_cards.png`, `unit_cards_selected.png`, and `unit_cards_locked.png` are each 576 × 368 pixels, arranged as 4 columns × 2 rows. Each card cell is 144 × 184 pixels. Order, left to right and then top to bottom:

| Cell | Card | Cost |
| --- | --- | ---: |
| (0,0) | PC | 50 |
| (1,0) | Year 1 | 100 |
| (2,0) | Gamer | 50 |
| (3,0) | Broken Charger | 75 |
| (0,1) | Weaboo | 150 |
| (1,1) | Year 2 | 150 |
| (2,1) | Year 3 | 250 |
| (3,1) | Year 4 | 350 |

Use the normal atlas for ready cards, the selected atlas while a card is selected, and the locked atlas for a card unavailable in the current stage. Grey/darken a ready card in Java when credits are insufficient or its cooldown is active. Draw a cooldown mask that shrinks as the cooldown counts down. The files are card art; the stage menu, top bar, click handling, and unlock rules are Java UI/model work.

**Year 1, Year 2, Year 3, and Year 4 are four separate placeable cards in this design. They are not upgrades.** The caps and matching star counts distinguish the student sprites. `year_cap.png` is a generic cap icon, not an instruction to implement a promotion mechanic. The latest design lists Year 4 as an optional extra if time allows.

`tile_placement_valid.png`, `tile_placement_invalid.png`, and `tile_selected.png` are transparent 64 × 64 borders. Scale each to 80 × 80 over the target tile. Valid means selected card + empty active tile + enough credits + ready cooldown. Invalid means placement is rejected. The model should not spend credits on an invalid tile.

### Placed sprites

Use files in `placed/` after a unit is on the grid. Each is the matching character PNG with a faint oval ground shadow already behind it, still transparent outside the character. Filenames end in `_placed.png`. For a selected card preview use the sprite without the shadow inside the card; for the board use the `_placed` file. Do not add another shadow unless you want a heavier look.

The base `unit_cards` and placed PNGs do not create the menu or board. Build the stage select screen, top bar, grid, health bars, speech messages, and buttons in Swing as described by the design.

## 3. Character and unit sprites

All character sprites have transparent backgrounds. Unless a file is explicitly a sheet, it is one pose.

| File | Game use |
| --- | --- |
| `pc.png` | PC defender, credit generator. |
| `student_base.png` | Reusable student body; useful as the source for the sick-student tint. |
| `year1_student.png` | Year 1 shooter with paper-ball cue. |
| `year2_student.png`, `year3_student.png`, `year4_student.png` | Separate shooters. Their mortarboard panels show exactly 2, 3, and 4 aligned stars. |
| `gamer.png` | Tank defender; does not attack. |
| `gamer_tired.png` | Replace `gamer.png` when Gamer health is below 50%. |
| `weaboo.png` | Random-attacker defender, with glasses and plushie. |
| `broken_charger.png` | Non-human plug creature. Use this, not a person holding a charger. |
| `sick_student.png` | Purple baseline enemy. |
| `sick_student_tissue.png` | Tissue-box enemy variant. |
| `sick_student_blanket.png` | Blanket enemy variant. |
| `prof_deadline.png` | Boss pose; draw about 96 × 112 screen pixels. |

`student_base.png` can be purple-tinted once at load if matching the design's reuse rule. The included `sick_student.png` is a ready-tinted version. Tissue and blanket files are ready-to-draw variants. Do not tint them again unless you intentionally want them darker/purpler.

For damage, briefly draw `damage_flash_white_overlay.png` over the entity for 3 ticks (0.1 s), or apply an equivalent white tint in code. Remove dead enemies over the design's 10-tick fade. `prof_damage_flash_overlay.png` is an older grey overlay; use the white overlay for damage flash and `stun_grey_overlay.png` for the Quiz stun state.

## 4. Animation sheets and exact row/frame meanings

### Students Year 1 to Year 4

- `year1_throw_strip.png`: 4 frames, one row: **0 wind-up, 1 release, 2 follow-through, 3 return**.
- `student_throw_actions_atlas.png`: 4 columns × 4 rows. Rows 0–3 are Year 1, 2, 3, 4 in that order. Columns use the same wind-up/release/follow-through/return sequence. Use this atlas for all four shooter cards.
- `animation_sheets/idle_year1_student.png`, `idle_gamer.png`, `idle_weaboo.png`: 4-frame idle/stance loops. Defenders stay in their placed tile; these are not movement between tiles.

Shooter game rules: Year 1 fires one paper ball every 1.5 s; Year 2 fires two balls every 1.5 s; Year 3 fires three every 1.2 s; Year 4 fires four every 1.0 s. Balls in a volley are 3 ticks apart. Each paper ball deals 20 damage, moves right at 5 tiles/s, and is removed on hit or beyond x=9.5. Year 4 deals 40 damage per ball to the boss. Use `paper_ball.png` for every ball; move projectile x in the model, not by scrolling the shooter frame.

### PC

`animation_sheets/pc_credit_pulse.png` is 4 frames in one row. Play it when a credit token is created. The PC creates its first 25-credit token 5 s after placement, then one every 8 s; it can have at most 3 uncollected tokens, and each token expires after 10 s. `credit.png` is the token to draw over the PC or at its tile. Clicking the token collects it.

### Gamer

Use `gamer.png` while health is at least 50%, and `gamer_tired.png` below 50%. Gamer blocks and absorbs bites; it has no attack animation or projectile.

### Weaboo

`weaboo_moves_atlas.png` is a 4-column × 4-row atlas. Rows are:

| Row | Move | Visual sheet |
| ---: | --- | --- |
| 0 | Plushie Barrage | `weaboo_plushie_barrage.png` |
| 1 | Dakimakura Smash | `weaboo_dakimakura_smash.png` |
| 2 | Dramatic Pose | `weaboo_dramatic_pose.png` |
| 3 | Kamehameha, Wi-Fi Edition | `weaboo_wifi_beam.png` |

Each standalone move PNG is a 4-frame horizontal strip, 640 × 192 total, with 160 × 192 per frame. The atlas rows are the same strips if you prefer one file. `plushie.png` is the separate projectile for Plushie Barrage: 3 projectiles, 4 ticks apart, 15 damage each, speed 4 tiles/s, rightward. Dakimakura Smash deals 120 damage to enemies overlapping the tile directly in front at the time of attack. Dramatic Pose does no damage and lasts 1 second. Wi-Fi beam instantly deals 150 damage to every enemy in Weaboo's row to his right; its visual lasts 0.5 s. Use `wifi_beam_effect_strip.png` for the long horizontal beam (4 frames, each 256 × 64); `wifi_beam_head.png` is the beam endpoint/impact. Scale the beam to the distance from Weaboo to the right edge. `weaboo_wifi_beam.png` is the character pose, not the full-width effect.

Weaboo rolls once every 2.5 s only when an enemy is in his row to the right: Plushie 40%, Smash 30%, Pose 20%, Beam 10%. Show the corresponding move name above him for 1.5 s. Animation frames do not choose the random move or deal damage.

### Sick-student enemies

- `animation_sheets/walk_sick_student.png`, `walk_sick_student_tissue.png`, `walk_sick_student_blanket.png`: one row of 4 walk frames for each variant. Flip horizontally if needed to face left. They walk from the right toward the left; their tile position comes from model x.
- `animation_sheets/enemy_bite_atlas.png`: 4 columns × 3 rows. Row 0 Sick Student, row 1 Tissue, row 2 Blanket. Four bite-mouth frames per row. Play while blocked and biting; stop walk-cycle selection while blocked.

Enemy reference stats: Sick Student 200 HP, 0.25 tiles/s; Tissue 120 HP, 0.45 tiles/s; Blanket 500 HP, 0.18 tiles/s. All bite for 20 damage/s (apply `20 / 30` per simulation tick). Sick variants use the purple sprites above. On death, use the 3-tick white damage flash if applicable, then fade out over 10 ticks.

### Broken Charger

`animation_sheets/charger_state_animation.png` is 4 columns × 4 rows; each cell is 160 × 192:

| Row | State | Visual | Game timing |
| ---: | --- | --- | --- |
| 0 | MINE | Normal charger | 0–6 s after placement. Only detonates when an enemy touches it. 600 damage to that enemy. |
| 1 | BOMB | Yellow flash | 6–14 s. Enemy touch or player click triggers a 3×3 explosion; enemies take 1800, defenders are safe. |
| 2 | UNSTABLE | Red sparks | 14–20 s. Enemy touch, click, or 20 s timeout triggers a 3×3 explosion; enemies take 1800 and defenders take 400. |
| 3 | EXPLOSION | Expanding blast | Show for 0.5 s (15 ticks), then remove the charger. |

Explosions deal a flat 500 to the boss. The charger has 100 HP; damage can destroy it without an explosion. A click while it is in MINE does nothing. The code must pause the charger timer when its defender is stunned, as specified by the design.

### Prof. Deadline

`prof_deadline_phase_sprites.png` has 3 side-by-side 160 × 192 cells: column 0 Lecture, column 1 Pop Quiz, column 2 Finals Rage. Use the boss health percentage to move phase forward only: Lecture above 60%, Pop Quiz at 60% down to above 30%, Finals Rage at 30% or below. Boss health is 9000; row 3 (middle), speed 0.10 tiles/s, doubled in Finals Rage; bite damage 150/s. Year 4 shots deal double; any charger explosion deals 500.

Each action strip below is one row of four 160 × 192 frames. Use the separate sheets for clearer selection; `animation_sheets/prof_deadline_actions.png` is an older combined overview atlas (4 columns × 5 rows: movement, chalk, summon, quiz, rage).

| File | What to show | Trigger / effect from design |
| --- | --- | --- |
| `prof_chalk_throw_strip.png` + `chalk.png` | Chalk wind-up and throw; chalk travels left. | Every 4 s, first defender hit takes 60 damage. Projectile speed 4 tiles/s. First chalk is 4 s after boss spawn. |
| `prof_summon_partner_strip.png` | Purple portal opens and a Sick Student emerges. | Every 8 s, spawn one Sick Student at boss x in random row 1, 2, 4, or 5. |
| `prof_pop_quiz_strip.png` | Professor holds quiz sheet; questions appear. | Pop Quiz phase only, every 10 s. Choose 3 random defenders (all if fewer), stun each for 3 s. |
| `prof_finals_rage_strip.png` | Red aura/lightning rage loop. | Finals Rage at ≤30% HP; boss speed becomes 0.20 tiles/s. |
| `prof_deadline_moved_up_strip.png` | Red speed streak and 2× warning. | Every 15 s in Finals Rage: other enemies move 1.5× for 5 s. |
| `prof_attendance_check_strip.png` | Professor checks a clipboard. | Every 20 s in Finals Rage: all PCs stop production for 4 s; existing tokens remain collectible. |
| `prof_deadline_defeat_strip.png` | B-grade / defeat pose. | Boss reaches 0 HP: clear other enemies, show defeat line for 3 s, then win. |

`prof_question_mark.png` is the separate marker to draw above stunned defenders. `stun_grey_overlay.png` is drawn over each stunned defender. A stunned unit does not attack or produce; its charger timer also pauses. `prof_deadline_moved_up_strip` and `prof_attendance_check_strip` are visuals only; implement the listed effects/timers in the model.

### Animation playback defaults

The design fixes gameplay durations but does not specify frame rate for every visual strip. To avoid scattered guesses, use centralized values. Suggested visual defaults (not gameplay rules): walk 6 ticks/frame (24-tick loop), idle 12 ticks/frame (48-tick loop), short action 3 ticks/frame (12 ticks total). Follow explicit durations where provided: Dramatic Pose 30 ticks; beam 15 ticks; explosion 15 ticks; Pop Quiz stun 90 ticks; boss defeat line 90 ticks. Advance animations from the game tick, not wall-clock time, and stop/hold at the last frame for one-shot poses where appropriate.

## 5. Projectiles, props, and environment

| File | Use |
| --- | --- |
| `paper_ball.png` | Student shooter projectile; move right in model. |
| `plushie.png` | Weaboo ranged projectile; move right in model. |
| `chalk.png` | Professor projectile; move left in model. |
| `wifi_beam_effect_strip.png` | Four-frame row-wide Wi-Fi beam visual, 256 × 64 per frame; total strip 1024 × 64. |
| `wifi_beam_head.png` | 64 × 64 beam impact/front edge. |
| `credit.png` | PC credit token worth 25. |
| `charger_spark.png` | Small extra unstable-state spark, if needed. |
| `year_cap.png` | Decorative cap icon for year cards; not an upgrade mechanic in the latest design. |
| `tile_classroom.png`, `tile_classroom_alt.png` | Alternating floor tiles. |
| `tile_stage.png` | Alternate stage floor tile. |

Projectile movement, collision, and hit removal are model logic. Paper balls and plushies are removed after hitting the first valid target or when beyond x=9.5. Chalk hits the first defender and is removed when px < -0.5.

## 6. Other asset files and older sheets

- `student_spritesheet.png` (in the pack root) is 320 × 192: base student at left, Year 1 at right. The matching individual PNGs are also present in this folder.
- `animation_sheets/promotion_sparkle.png` is a generic 4-frame sparkle. The latest design makes Years 1–4 separate cards, not upgrades; use this only as a decorative placement effect if desired.
- `prof_damage_flash_overlay.png` is a greyed boss overlay kept from an earlier pass. Prefer `damage_flash_white_overlay.png` for damage flashes and `stun_grey_overlay.png` for Pop Quiz.
- `SPRITE_MAP.md` is a short historical map; this README is the complete guide.
- `StudentSprite.java` in the pack root demonstrates PNG loading, purple tinting, and nearest-neighbor drawing. It is a helper, not the full game or animation controller.

## 7. What the PNG pack does not implement

The PNGs do not create Swing screens, handle clicks, select cards, enforce unlocks/cooldowns, move entities, change boss phases, run timers, spawn projectiles/enemies, detect collision, apply damage, pause the simulation, or save progress. Use the game model and tick loop for those. The screen layout and text are in `GAME_DESIGN_README.md`; the sprite sheets only supply images to draw.

## 8. Card readiness and stage unlock lookup

The top-bar cooldown lengths below are seconds. Start each available card ready. A card is greyed when its cooldown remains or the player cannot afford it. These mechanics are not baked into card PNGs.

| Card | Health | Cost | Cooldown |
| --- | ---: | ---: | ---: |
| PC | 300 | 50 | 5 s |
| Year 1 | 300 | 100 | 5 s |
| Year 2 | 300 | 150 | 7 s |
| Year 3 | 400 | 250 | 10 s |
| Year 4 | 500 | 350 | 15 s |
| Gamer | 1500 | 50 | 15 s |
| Weaboo | 300 | 150 | 10 s |
| Broken Charger | 100 | 75 | 20 s |

Stage card availability from the design:

| Stage | Cards available |
| --- | --- |
| 0 Orientation Day | PC, Year 1 |
| 1 Week 1 | PC, Year 1, Gamer, Year 2 |
| 2 Midterm | PC, Year 1, Year 2, Year 3, Gamer, Weaboo, Broken Charger |
| 3 Finals | All cards, including Year 4 |

Card order in the PNG atlas is fixed as documented in section 2; filter or grey cards according to this stage table. Stage select buttons and the stage-select background are drawn by Swing, not included as PNGs.

## 9. Timing reference

The simulation runs at 30 ticks/second. Convert seconds to ticks by multiplying by 30. Gameplay timing and art-frame playback are separate: gameplay events use the exact values in this guide; frame playback uses the centralized visual defaults above unless the design gives an explicit animation duration. Pause the animation counters whenever the game is paused.

Boss phase banners last 2 s (60 ticks). Use `LECTURE`, `POP QUIZ!`, and `FINALS RAGE!` as specified by the design. The boss health bar text follows its phase: `Prof. Deadline - Lecture`, `Prof. Deadline - Pop Quiz`, or `Prof. Deadline - Finals Rage`. These labels are text rendered in Swing, not separate images.
