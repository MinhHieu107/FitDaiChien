
## Standalone Weaboo attack strips

For easier use without cropping the full atlas, these are also provided as individual 4-frame horizontal strips, each 640 × 192 pixels (four 160 × 192 cells): `weaboo_plushie_barrage.png`, `weaboo_dakimakura_smash.png`, `weaboo_dramatic_pose.png`, and `weaboo_wifi_beam.png`.

## Separate Wi-Fi beam effect

- `wifi_beam_effect_strip.png`: transparent 4-frame horizontal beam effect, 256 × 64 pixels per frame. The effect grows from charge to full row beam and ends in an impact flash. Scale or crop the beam to the Weaboo-to-row-end distance in the game.
- `wifi_beam_head.png`: separate 64 × 64 transparent beam endpoint/impact sprite.
- `weaboo_wifi_beam.png`: character attack poses with a beam cue; use the separate effect strip for the long row-wide beam.

## Prof. Deadline phase and action art (GAME_DESIGN_README)

- `prof_deadline_phase_sprites.png`: three side-by-side boss looks in order: Lecture, Pop Quiz, Finals Rage. Draw a cell at 160 × 192 source size and scale to the boss draw size (the design suggests about 96 × 112 screen pixels).
- `prof_chalk_throw_strip.png`: four-frame chalk throw pose; draw `chalk.png` as a separate projectile moving left. The game fires it every 4 seconds and applies damage to the first defender hit.
- `prof_summon_partner_strip.png`: four frames of a portal opening and a purple student appearing. The game spawns the partner at the boss x position in a random row from 1, 2, 4, or 5 every 8 seconds.
- `prof_pop_quiz_strip.png`: four frames with the quiz sheet and question marks. `prof_question_mark.png` is the separate indicator to draw over each selected defender. The model chooses up to three defenders, stuns them for 3 seconds, and uses `stun_grey_overlay.png` while stunned.
- `prof_finals_rage_strip.png`: four-frame rage aura/flash. `prof_deadline_moved_up_strip.png` visualizes the 1.5× enemy-speed event; `prof_attendance_check_strip.png` shows the clipboard action. Their timers and effects are controlled by the game model.
- `prof_deadline_defeat_strip.png`: four-frame end pose to use during the three-second defeat line.

Each action strip has four 160 × 192 cells. Use a tick counter to advance the visual frames; keep attack timing and damage from the design in game logic. Professor attack timers start when the boss spawns, so the first chalk should be shown after four seconds.
