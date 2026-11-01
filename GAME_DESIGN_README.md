# Flu Season Defense - Complete Game Design

This file describes one Java game completely: rules, numbers, stages, screens, text and art. It is written so that another AI or developer can build the game from it without guessing. All numbers are first-pass values and must be easy to tune (keep them in one constants/data place).

---

## 1. Pitch

A Plants vs Zombies style tower defense game with a university theme.

- The player is a student defending a lecture hall. Classmates caught the flu and turned **purple**. They walk in from the right.
- The player earns **credits** and places **student defenders** on a grid. Enemies walk left. If one reaches the left edge of the grid, the stage is lost.
- The last stage ends with a boss: the professor, **Prof. Deadline**.
- Tone: silly and light. Nobody is hurt. No food jokes, no coding jokes.

## 2. Technical constraints (this is a university Java course project)

- Language: Java 17+. GUI: Swing/AWT (`JFrame`, `JPanel`, `Graphics2D`). No game engines. JUnit 5 for tests only.
- Game logic must contain **no Swing code** so it can be unit tested.
- Fixed simulation rate: **30 ticks per second**. All durations below are given in seconds; ticks = seconds x 30.
- Threading: the simulation runs on its own game-loop thread. The Swing thread never changes game state directly. Mouse clicks become `Command` objects in a thread-safe queue, which the loop thread processes at the start of each tick. After each tick the loop thread publishes an immutable snapshot (via a `volatile` reference) that the Swing thread draws.
- Stages are loaded from text files (format in section 14). Progress is saved to `progress.txt`.
- Not in scope: networking, database, multiplayer, score/leaderboard, level editor, shovel/sell tool.

---

## 3. Screen and grid constants

| Name | Value |
| ---- | ----- |
| Window size | 900 x 600 px, not resizable |
| Grid | 5 rows x 9 columns |
| Tile size | 80 x 80 px |
| Grid origin (top-left) | x = 90, y = 100 |
| Grid area | x 90..810, y 100..500 |
| Top bar | y 0..100: unit cards, credits, pause button |
| Bottom bar | y 500..600: message box, wave progress, boss health bar |
| Left zone | x 0..90: lecture hall door (decoration). Lose line is the grid's left edge (x = 90) |
| Right zone | x 810..900: where enemies enter |
| Tile pixel position | x = 90 + col*80, y = 100 + row*80 (row, col are 0-based) |
| Sprite draw scale | base sprite 32x36 px drawn at 2x (64x72 px), centered in the tile, nearest-neighbor scaling |

Stage files and on-screen text number rows 1..5 (top to bottom). Code uses 0..4.

## 4. Coordinate and hitbox rules (model uses tile units)

- Horizontal position `x` is a decimal in tile units: `0.0` = left edge of the grid, `9.0` = right edge. Rows are integers.
- A defender at column `c` has hitbox `[c+0.1, c+0.9]`.
- A normal enemy at position `x` has hitbox `[x, x+0.6]`. The boss has hitbox `[x, x+1.0]`.
- A projectile at position `px` has hitbox `[px, px+0.2]`. Two things collide when they are in the same row and their x ranges overlap.
- Enemies spawn at `x = 9.6` (just off the right edge) in the row given by the stage.
- An enemy is **blocked** when its hitbox overlaps a defender's hitbox in its row. While blocked it stops and bites that defender (the one closest to it).
- **Lose:** any enemy with `x <= 0.0`.

## 5. Tick order (the simulation does these steps every tick, in this order)

1. Drain the command queue and apply commands (place unit, collect credit, detonate charger, pause, etc.).
2. Spawn scheduled enemies and show scheduled banners for this time.
3. Update credit tokens (expiry) and PC production timers.
4. Update defenders (cooldowns, attack timers, charger state, stun timers). Create projectiles and effects.
5. Move projectiles.
6. Move enemies; blocked enemies bite instead of moving.
7. Resolve collisions and apply damage.
8. Remove dead entities; handle boss phase changes.
9. Check win and lose.
10. Publish the render snapshot.

---

## 6. Economy and cards

- **Credits** are the currency. Start credits depend on the stage (section 13). Maximum 9999. Credits do not carry over between stages.
- **Credit token:** worth 25 credits. A PC creates one on its tile. The player collects it by clicking it. A token disappears after 10 seconds if not collected. A PC will not create a new token while it already has 3 uncollected tokens.
- **Cards:** each unit available in the stage has a card in the top bar. A card is disabled if credits < cost or the card is cooling down. Cards start ready at stage start.
- **Placing:** click a card to select it (highlighted), then click an **empty active tile**. Credits are spent and the card's cooldown starts. Placing is rejected (nothing is spent) if the tile is occupied, the row is not active, credits are too low or the card is cooling down. Placing is allowed even if an enemy is overlapping the tile.
- Press `Esc` or click the selected card again to cancel the selection.
- Defenders only leave the board by dying (health 0) or by a charger explosion. There is no sell tool.

### Click priority (when the player clicks the grid or top bar)

1. UI buttons and cards.
2. A credit token under the cursor (collect it, do not place anything).
3. A Broken Charger in BOMB or UNSTABLE state on the clicked tile (detonate it).
4. An empty tile with a selected card (place the unit).

---

## 7. Defenders

| Unit | Role | Cost | Health | Cooldown (s) | Behavior |
| ---- | ---- | ---- | ------ | ------------ | -------- |
| PC | Generator | 50 | 300 | 5 | First token 5 s after placement, then one token every 8 s |
| Year 1 student | Shooter | 100 | 300 | 5 | 1 paper ball per shot, shot every 1.5 s |
| Year 2 student (cap, 2 stars) | Shooter | 150 | 300 | 7 | 2 paper balls per shot, every 1.5 s |
| Year 3 student (cap, 3 stars) | Shooter | 250 | 400 | 10 | 3 paper balls per shot, every 1.2 s |
| Year 4 student (cap, 4 stars) | Shooter | 350 | 500 | 15 | 4 paper balls per shot, every 1.0 s, **double damage to the boss** |
| Gamer | Tank | 50 | 1500 | 15 | Does not attack. Sprite changes to "tired" under 50% health |
| Weaboo | Random attacker | 150 | 300 | 10 | See 7.2 |
| Broken Charger | Trap | 75 | 100 | 20 | See 7.3 |

Year 1 to 4 are **four separate units** the player places directly. They are not upgrades. In code they can be one `Student` class with a year value that reads its stats from a table. The cap with stars is what makes the years visually different.

### 7.1 Shooters (Year 1 to 4)

- A shooter fires only when at least one enemy is in its row and to its right (inside the board).
- The attack timer counts up continuously. When it reaches the interval and a target exists, the shooter fires and the timer resets. If no target exists the timer stays at the interval (ready to fire immediately).
- A shot with N balls creates the balls one after another, 3 ticks (0.1 s) apart, all starting at `px = col + 0.8` in the shooter's row.
- Paper ball: 20 damage, speed 5 tiles/s. Against the boss, a Year 4's balls deal 40 damage.

### 7.2 Weaboo

- Every 2.5 s, if an enemy is in his row and to his right, he rolls one random number 0..99 and uses a move:

| Roll | Move | Effect |
| ---- | ---- | ------ |
| 0..39 (40%) | Plushie Barrage (ranged) | 3 plushies, 4 ticks apart, 15 damage each, speed 4 tiles/s, same row |
| 40..69 (30%) | Dakimakura Smash (melee) | 120 damage to every enemy overlapping the tile directly in front (column + 1) at that moment. Hits nothing if nobody is there |
| 70..89 (20%) | Dramatic Pose (dud) | Does nothing. Plays a 1 s pose animation |
| 90..99 (10%) | Kamehameha, Wi-Fi Edition (beam) | Instantly 150 damage to every enemy in his row to his right. Beam visual lasts 0.5 s |

- When a move is used, float its name above him for 1.5 s (names in section 17).

### 7.3 Broken Charger

One timer counts seconds since placement. Three states:

| State | Time on field | Look | Trigger | Effect |
| ----- | ------------- | ---- | ------- | ------ |
| MINE | 0 to 6 s | normal sprite | An enemy touches it (enemy hitbox overlaps the charger's) | Only the touching enemy takes 600 damage. The charger is used up |
| BOMB | 6 to 14 s | flashes yellow | An enemy touches it, **or** the player clicks it | 3x3 explosion. Enemies in the area take 1800. Defenders are safe |
| UNSTABLE | 14 to 20 s | flashes red | An enemy touches it, the player clicks it, **or** the timer reaches 20 s | 3x3 explosion. Enemies in the area take 1800. **Every defender in the area takes 400** |

- Explosion area: rows `r-1..r+1` and columns `c-1..c+1` around the charger's tile, clipped to the board. An enemy is hit if its hitbox overlaps any of those tiles. The charger itself is removed.
- Against the boss, **any** charger explosion deals a flat 500 damage.
- Clicking a charger in MINE state does nothing.
- The charger has 100 health. If it is destroyed by damage (for example chalk) it disappears without exploding.
- Explosion visual lasts 0.5 s.

---

## 8. Enemies

All normal enemies are the **student sprite tinted purple** (blend each non-transparent pixel 50% with `0x8A2BE2`, done once when the image is loaded). Variants add a small overlay.

| Enemy | Health | Speed (tiles/s) | Bite damage per second | Look |
| ----- | ------ | --------------- | ---------------------- | ---- |
| Sick Student | 200 | 0.25 | 20 | purple student |
| Student with Tissue Box | 120 | 0.45 | 20 | purple student holding a tissue box |
| Student in Blanket | 500 | 0.18 | 20 | purple student wrapped in a blanket |

- Bite damage is applied each tick as `damagePerSecond / 30`.
- Enemies take damage flash (white tint for 3 ticks) and fade out over 10 ticks when they die.

---

## 9. Boss: Prof. Deadline

| Property | Value |
| -------- | ----- |
| Health | 9000 |
| Row | 3 (the middle row), spawns at `x = 9.8` |
| Speed | 0.10 tiles/s (0.20 in Finals Rage) |
| Bite damage | 150 per second |
| Hitbox | `[x, x+1.0]` |
| Sprite | larger than normal, about 48x56 px |
| Damage rules | Year 4 shots do double. Any charger explosion does a flat 500. Other damage is normal |

The boss uses a phase based on his current health percentage. The phase can only move forward.

| Phase | Health | Attacks |
| ----- | ------ | ------- |
| LECTURE | above 60% | **Chalk:** every 4 s throws a chalk projectile left along his row (speed 4 tiles/s, 60 damage to the first defender it hits). **Group Project Partner:** every 8 s spawns one Sick Student at the boss's current `x` in a random row from {1, 2, 4, 5} |
| POP_QUIZ | 60% down to above 30% | Keeps chalk and summon. **Pop Quiz:** every 10 s, 3 random defenders (all, if fewer than 3) get a "?" and are **stunned for 3 s**. A stunned defender does not attack, produce, or change charger state timers, and is drawn greyed |
| FINALS_RAGE (optional, implement last) | 30% and below | Keeps everything above and moves at 0.20. **Deadline Moved Up:** every 15 s all other enemies move 1.5x faster for 5 s. **Attendance Check:** every 20 s all PCs stop producing for 4 s (tokens already out can still be collected) |

- A banner shows for 2 s when the phase changes (text in section 17).
- All attack timers start when the boss spawns. The first chalk comes after 4 s.
- When the boss reaches 0 health: all other enemies disappear, the defeat line is shown for 3 s, then the stage is WON.

---

## 10. Projectiles

| Projectile | Owner | Damage | Speed (tiles/s) | Direction | Notes |
| ---------- | ----- | ------ | --------------- | --------- | ----- |
| Paper ball | Year 1-4 | 20 (40 for Year 4 vs boss) | 5 | right | Removed on hit or when `px > 9.5` |
| Plushie | Weaboo | 15 | 4 | right | Same removal rules |
| Chalk | Boss | 60 | 4 | left | Hits the first defender in the row; removed when `px < -0.5` |

A projectile hits only the first valid target it overlaps (nearest to its owner), then disappears.

---

## 11. Game states and screens

| State | What the player sees | Buttons |
| ----- | -------------------- | ------- |
| MENU | Title "Flu Season Defense" | Play, Quit |
| STAGE_SELECT | 4 stage buttons; locked stages greyed | Back |
| PLAYING | The game (section 12) | Pause (button or `P`) |
| PAUSED | Dimmed game, simulation stopped | Resume, Restart stage, Back to menu |
| WON | "Stage cleared!" (or the tutorial/final text) | Next stage, Back to menu |
| LOST | Lose text | Retry, Back to menu |
| ENDING | After the final stage: "COURSE PASSED" and the credits line | Back to menu |

## 12. In-game screen layout

- **Top bar (y 0..100):** cards left to right starting at `x = 10`, each 72x90 px with an 8 px gap (`x = 10 + i*80`, `y = 5`). Each card shows the unit sprite, its cost, a dark cooldown overlay that shrinks as it cools, and greys out when unaffordable. A "NEW" badge shows on cards introduced in this stage.
- **Credits panel:** `x 650..780`, shows a credit icon and the number.
- **Pause button:** `x 800..890`, `y 10..50`.
- **Grid:** 5x9 tiles, alternating two light floor colors. Inactive rows (tutorial) are dark grey and cannot be used.
- **Bottom bar (y 500..600):** a message box (speech/tips, up to 2 lines), a thin wave progress bar (elapsed time / last spawn time), and during the boss fight a boss health bar `x 200..700, y 560..580` with the phase name.
- **Banners:** big centered text, 2 s, fade out (for huge waves, phase changes, stage title).
- Floating texts (`+25`, move names) rise and fade over 1.5 s.

### Input

| Input | Action |
| ----- | ------ |
| Left click | Everything in section 6 (cards, tokens, charger, place) |
| `Esc` | Cancel selected card |
| `P` | Pause / resume |

---

## 13. Stages

Stage start credits do not carry over. A stage is **WON** when every scheduled enemy has spawned and none are alive (stage 4: when the boss dies). Clearing stage N unlocks stage N+1. The `time` below is seconds after the stage starts (tutorial: after step 4 starts).

### Stage 0 - Orientation Day (tutorial)

- Active rows: 2, 3, 4 (rows 1 and 5 are dark and unusable). Start credits: 150.
- Cards: PC, Year 1. Enemies: Sick Student.
- Enemies do not spawn until the player finishes step 3. Messages appear in the bottom bar, and the relevant card or tile is highlighted.

| Step | Message | Completes when |
| ---- | ------- | -------------- |
| 1 | "Welcome to class! Click the PC card, then click a tile to place it." | A PC is placed |
| 2 | "PCs make credits. Click the credit icon when it appears." | One token is collected |
| 3 | "Place a Year 1 student in a row. They throw paper at sick students in that row." | A Year 1 is placed |
| 4 | "Here they come! Don't let anyone reach the left edge." | All enemies are defeated |
| 5 | "Nice! Class survived. Next up: Week 1." | Shown on the WON screen |

Spawns (after step 4 starts): `0 s Sick row 3`, `12 s Sick row 3`, `24 s Sick row 2`.

### Stage 1 - Week 1: Just a Little Cough

- Active rows: all 5. Start credits: 150.
- Cards: PC, Year 1, Gamer, Year 2. New: Gamer, Year 2. Enemies: Sick Student, Student with Tissue Box.
- Tip (first 6 s): "Gamer soaks up bites. Put him in front of your shooters."

| Time (s) | Enemy | Row |
| -------- | ----- | --- |
| 20 | Sick | 3 |
| 32 | Sick | 2 |
| 44 | Sick | 4 |
| 70 | Sick | 1 |
| 74 | Sick | 5 |
| 78 | Tissue | 3 |
| 90 | Sick | 2 |
| 94 | Sick | 4 |
| 120 | Sick | 3 |
| 120 | Sick | 2 |
| 124 | Tissue | 4 |
| 128 | Sick | 1 |
| 128 | Tissue | 5 |

13 enemies. Expected length: about 2.5 to 3 minutes.

### Stage 2 - Midterm: Is This On The Exam?

- Active rows: all 5. Start credits: 200.
- Cards: PC, Year 1, Year 2, Year 3, Gamer, Weaboo, Broken Charger. New: Year 3, Weaboo, Broken Charger. Enemies: Sick, Tissue, Blanket.
- Tips: "The charger changes over time. Click it before it turns red!" and "Weaboo is random. Embrace the chaos."
- Banner at 150 s: "A huge wave of students is approaching!"

| Time (s) | Enemy | Row |
| -------- | ----- | --- |
| 15 | Sick | 3 |
| 22 | Sick | 2 |
| 29 | Tissue | 4 |
| 36 | Sick | 5 |
| 43 | Sick | 1 |
| 65 | Blanket | 3 |
| 72 | Sick | 2 |
| 76 | Sick | 4 |
| 85 | Tissue | 1 |
| 85 | Tissue | 5 |
| 110 | Blanket | 2 |
| 110 | Blanket | 4 |
| 116 | Sick | 3 |
| 120 | Tissue | 1 |
| 120 | Tissue | 5 |
| 128 | Sick | 2 |
| 128 | Sick | 4 |
| 150 | Sick | 1, 2, 3, 4, 5 (five enemies, one per row) |
| 156 | Blanket | 3 |
| 156 | Tissue | 2 |
| 156 | Tissue | 4 |

25 enemies. Expected length: about 3.5 minutes.

### Stage 3 - Finals: The Professor Arrives

- Active rows: all 5. Start credits: 250.
- Cards: PC, Year 1, Year 2, Year 3, Year 4, Gamer, Weaboo, Broken Charger. New: Year 4. Enemies: Sick, Tissue, Blanket, boss.
- Tip: "Year 4 deals double damage to the professor."
- Boss banner at 100 s: "Today's lecture is a surprise."

| Time (s) | Enemy | Row |
| -------- | ----- | --- |
| 10 | Sick | 1 |
| 18 | Sick | 2 |
| 26 | Sick | 4 |
| 34 | Tissue | 5 |
| 42 | Blanket | 3 |
| 60 | Sick | 2 |
| 60 | Sick | 4 |
| 68 | Tissue | 1 |
| 68 | Tissue | 5 |
| 76 | Blanket | 2 |
| 76 | Blanket | 4 |
| 100 | **Boss Prof. Deadline** | 3 |
| 105 | Tissue | 1 |
| 115 | Tissue | 5 |
| 125 | Sick | 2 |
| 125 | Sick | 4 |

After the boss dies the stage is won and the ENDING screen follows.

---

## 14. Stage file format (suggested)

One text file per stage in `resources/stages/` (`stage0.txt` to `stage3.txt`). One `key=value` per line; `#` starts a comment; blank lines ignored. Rows are 1-based.

```text
name=Week 1: Just a Little Cough
activeRows=1,2,3,4,5
startCredits=150
cards=PC,YEAR1,GAMER,YEAR2
newCards=GAMER,YEAR2
tip=0,6,Gamer soaks up bites. Put him in front of your shooters.
banner=150,A huge wave of students is approaching!
spawn=20,SICK,3
spawn=32,SICK,2
spawn=124,TISSUE,4
boss=100,PROF,3
tutorial=true
```

- `spawn=timeSeconds,TYPE,row` where TYPE is `SICK`, `TISSUE` or `BLANKET`.
- `boss=timeSeconds,PROF,row` only in the final stage.
- `tip=startSeconds,durationSeconds,text`.
- `tutorial=true` makes spawn times count from the start of tutorial step 4 and enables the tutorial steps.
- Card ids: `PC, YEAR1, YEAR2, YEAR3, YEAR4, GAMER, WEABOO, CHARGER`.
- A missing file, a bad line or an unknown id must **not crash the game**: show a clear error message and return to the menu (or skip the bad line and log it).
- Progress file `progress.txt` holds one integer: the highest unlocked stage (0..3). If missing or invalid, treat it as 0.

---

## 15. Text used in the game

**Intro (first launch, before the tutorial):** "Flu season hit campus. Midterms hit harder. Your classmates are turning purple. Hold the lecture hall."

**When placing a unit** (message box, 2 s):

| Unit | Line |
| ---- | ---- |
| Year 1 | "I just got here..." |
| Year 2 | "I know where the printer is." |
| Year 3 | "Do not ask me about group projects." |
| Year 4 | "I've seen worse deadlines." |
| Gamer | "Tanking since 2015. Ez." |
| PC | "Loading credits... 99%... 99%..." |
| Weaboo | "This is just like episode 12!" |
| Broken Charger | "Only works if you hold the cable at an angle." |

**Charger state lines** (floating text): BOMB: "Battery: 1%. Danger: 100%." UNSTABLE: "Why is it hot?"

**Weaboo move names** (floating text): "Plushie Barrage!", "Dakimakura Smash!", "...*dramatic pose*", "Kamehameha, Wi-Fi Edition!"

**Enemy lines** (message box, only the first time that type appears in a stage): Sick "*cough* Is attendance mandatory?", Tissue "*sniff* Wait for me!", Blanket "Five more minutes..."

**Boss lines:** spawn "Today's lecture is a surprise." Phase 2 "Pop quiz! Close your notes!" Phase 3 "The deadline has been moved. To yesterday." Defeat "...Fine. Everyone gets a B. Class dismissed."

**Screens:** Stage cleared "Stage cleared!" Final win "COURSE PASSED". Lose "You have been marked absent. See you next semester."

---


---

## 16. Banners and tips used in the UI

- Phase 2 banner: "POP QUIZ!" Phase 3 banner: "FINALS RAGE!"
- Boss health bar label: "Prof. Deadline - Lecture", "- Pop Quiz", "- Finals Rage".
- Stage title banner at the start of each stage: the stage name.

---

## 17. Edge cases the implementation must handle

1. Clicking an occupied tile with a card selected: nothing is spent, selection stays.
2. Placing with 0 cooldown but not enough credits: rejected, show the card shake or a message.
3. A PC with 3 uncollected tokens waits until one is collected or expires.
4. Two enemies touching one charger at the same time: the explosion rules apply once.
5. Boss killed while a charger explosion is also resolving: process damage first, then the win check.
6. Pausing stops all timers, tokens, cooldowns and the boss attack timers.
7. Restart resets everything in the stage, including cooldowns and the tutorial steps.
8. Closing the window stops the game-loop thread and any sound threads cleanly.
9. Losing and winning on the same tick: **lose wins**.
10. Stunned PCs stop producing while stunned; stunned chargers pause their timers.

---

## 18. Suggested build order

1. Window, grid drawing, state enum, game-loop thread, command queue, snapshot.
2. Cards, credits, placing a PC, tokens, collecting.
3. Year 1, paper ball, Sick Student (move, bite), collision, lose line, win check. Tutorial playable here.
4. Stage file loader and `progress.txt`. Stage select screen.
5. Gamer, Year 2 to 4 (stats table), Tissue and Blanket enemies.
6. Broken Charger (three states), Weaboo (random table).
7. Boss with LECTURE and POP_QUIZ phases, boss health bar, ending screen.
8. Real sprites, messages and banners, FINALS_RAGE, sound (last, optional).

Cut order if time runs out: sound, Finals Rage, Weaboo beam, Year 3.
