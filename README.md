# Flu Season Defense

> Draft README. Search for `TODO` and fill in every one before submitting.
> Only keep features, patterns and experiments that you really implemented.
> Never write test results or measurements you did not actually run.

---

# 1. Project Information

| Information   | Description               |
| ------------- | ------------------------- |
| Project Name  | Flu Season Defense        |
| Group         | TODO (e.g. Group01)       |
| Course        | Java Software Development |
| Academic Year | 2026-2027                 |
| Instructor    | TODO                      |
| Members       | TODO                      |

### Team Members

| Student ID | Name | Main Responsibility |
| ---------- | ---- | ------------------- |
| TODO       | TODO | TODO                |
| TODO       | TODO | TODO                |

---

# 2. Project Overview

## 2.1. Problem

A real-time tower defense game (Plants vs Zombies style) written in Java Swing. The player places student defenders on a grid to stop waves of sick (purple) students and a final boss, Prof. Deadline, before they reach the left edge of the lecture hall. The technical problem is to keep a real-time simulation (movement, shooting, collisions, timers) smooth and correct while the user keeps clicking, without race conditions between the GUI thread and the game logic.

## 2.2. Motivation

A tower defense game naturally needs state management, event handling, animation, collision detection and concurrency, which are the main topics of the course. The campus-flu theme only gives the game its look; the focus is the architecture.

## 2.3. Objectives

1. Build a playable tower defense game with a tutorial, normal stages and a boss stage.
2. Separate game logic, rendering and input so each can be tested alone.
3. Run the simulation on its own thread without race conditions with the Swing thread.
4. Load stages from files and save progress.
5. Test the logic with JUnit and measure performance with real experiments.

## 2.4. Scope

**Included**
- 5x9 grid, credits, cooldowns, unit placement.
- Defenders: PC (generator), Year 1 student (shooter), Gamer (tank), Broken Charger (timed mine/bomb), Weaboo (random attacks), Year 2 cap (promotion).
- Enemies: Sick Student and variants. Boss: Prof. Deadline with boss phases.
- Stages: tutorial, 2 normal stages, boss stage. Unlock progress is saved.

**Not included**
- Multiplayer, networking, database, level editor.
- Optional extras only if time allows: Year 3 and Year 4 caps, third boss phase, sound effects.

## 2.5. Requirements

**Functional**
- FR1: The player can select a unit card and place it on an empty tile if credits and cooldown allow.
- FR2: PCs generate credits over time; the player can collect them.
- FR3: Shooters fire projectiles along their row when an enemy is in that row.
- FR4: Enemies walk left, attack defenders in their way, and cause a loss if they cross the left edge.
- FR5: The Broken Charger changes behavior over time (mine, bomb, unstable) and can damage the player's own units.
- FR6: A Year 1 student can be promoted by placing a cap on it.
- FR7: The boss has phases that change with its remaining health.
- FR8: Stages are loaded from files; cleared stages unlock the next stage and the progress is saved.
- FR9: The game has menu, playing, paused, won and lost states.

**Non-functional**
- NFR1: The simulation runs at a fixed 30 ticks per second (TODO: confirm the measured rate).
- NFR2: The GUI never freezes while the simulation runs.
- NFR3: No race conditions or `ConcurrentModificationException` during play.
- NFR4: A missing or malformed stage or save file must not crash the game.
- NFR5: Game logic has no Swing code so it can be unit tested.

---

# 3. Main Features

- Grid-based tower defense with credits and cooldowns.
- Tutorial stage with guided pop-ups.
- Stage progression where each stage unlocks new units.
- Broken Charger with three time-based states.
- Weaboo with weighted random attacks (40% plushies, 30% melee, 20% nothing, 10% beam).
- Boss with phases.
- Stages loaded from text files and progress saved to a file.
- Pause, win and lose screens.

---

# 4. System Architecture

```text
+------------------+      Commands       +--------------------+
|  Swing thread    | ------------------> |  Command queue     |
|  GamePanel       |                     |  (thread-safe)     |
|  InputHandler    |                     +---------+----------+
+--------^---------+                               |
         | reads snapshot                          v
+--------+---------+                     +---------+----------+
| RenderSnapshot   | <------------------ |  Game loop thread  |
| (volatile ref)   |   publishes each    |  GameLoop          |
+------------------+   tick              |  GameModel.update  |
                                         +---------+----------+
                                                   |
                                  +----------------+----------------+
                                  |                                 |
                          +-------v------+                  +-------v-------+
                          | StageLoader  |                  | ProgressStore |
                          | (stage files)|                  | (progress.txt)|
                          +--------------+                  +---------------+
```

- **GamePanel / InputHandler (Swing thread):** draw the latest snapshot; turn mouse clicks into `Command` objects.
- **Command queue:** the only way the GUI changes the game. Clicks never touch the model directly.
- **GameLoop (game thread):** every tick, drains the command queue, calls `GameModel.update()`, builds a snapshot and asks Swing to repaint.
- **GameModel:** all rules (spawning, movement, collision, credits, boss phases). No Swing code.
- **StageLoader / ProgressStore:** read stage definitions and save/load unlocked stages.
- **SoundPlayer (optional):** plays clips on a small thread pool.

---

# 5. Java Design

## 5.1. Class Structure

| Class | Responsibility |
| ----- | -------------- |
| `Main` | Starts the window on the Swing thread. |
| `GameState` (enum) | MENU, STAGE_SELECT, PLAYING, PAUSED, WON, LOST. |
| `GameModel` | Grid, entities, credits, waves, `update()` each tick. |
| `GameLoop` | Fixed-rate loop on a background thread. |
| `Command` (interface) | An action requested by the player, applied by the loop thread. |
| `Defender` (abstract) | Base for `Pc`, `Student` (year 1-2), `Gamer`, `Weaboo`, `BrokenCharger`. |
| `Enemy` / `Boss` | Health, speed, row; `Boss` adds `BossPhase` and attack timers. |
| `Projectile` | Paper ball, plushie, chalk; moves each tick. |
| `StageConfig` | Data for one stage (rows, waves, unlocked cards, tutorial flag). |
| `StageLoader` | Reads `StageConfig` from a text file. |
| `ProgressStore` | Saves and loads unlocked stages. |
| `GamePanel` | Drawing only, from a `RenderSnapshot`. |
| `InputHandler` | Mouse listener that creates `Command` objects. |

## 5.2. OOP Design

- **Encapsulation:** entity fields are private; the model changes them only through methods such as `takeDamage()`.
- **Inheritance:** `Defender` and `Enemy` base classes with concrete subclasses; `Boss extends Enemy`.
- **Polymorphism:** the model calls `update()` on every `Defender` without knowing its type.
- **Abstraction:** `Defender` defines the shared behavior; subclasses fill in what differs.
- **Interfaces:** `Command`, plus small capability interfaces such as `Damageable` and `Attacker` (TODO: keep only those you implement).
- **Composition:** a `Student` has a `Cap`/year level; stage data is a separate `StageConfig` object rather than hard-coded.

## 5.3. SOLID Principles

TODO: keep only what is true in your code.

- **Single Responsibility:** `GamePanel` only draws, `GameModel` only runs rules, `ProgressStore` only saves.
- **Open/Closed:** a new defender type is a new subclass; the game loop does not change.
- **Liskov Substitution:** any `Defender` subclass can be used where a `Defender` is expected.
- **Interface Segregation:** small interfaces (`Damageable`, `Attacker`) instead of one large one.
- **Dependency Inversion:** `GameLoop` depends on an interface for the model, not on the Swing panel.

## 5.4. Design Patterns

TODO: keep only the patterns you implement.

| Pattern | Problem | Solution | Where |
| ------- | ------- | -------- | ----- |
| State | Behavior changes with a state (charger, boss, game screens). | Enums with state-specific logic. | `ChargerState`, `BossPhase`, `GameState` |
| Strategy | Weaboo needs interchangeable random attacks. | Each attack is an object chosen by a weighted roll. | `WeabooAttack` |
| Factory | Units and enemies are created from stage data. | A factory builds the right class from a name. | `UnitFactory`, `EnemyFactory` |
| Command | The GUI must not change the model directly. | Input becomes `Command` objects processed by the loop. | `Command`, `PlaceUnitCommand` |

---

# 6. Technical Implementation

Only technologies that are actually used are listed.

## 6.1. I/O

- Text I/O with `java.nio.file.Files` to read stage files and read/write `progress.txt`.
- Missing or malformed files are handled with exceptions and a safe default.

## 6.2. Concurrency

- **Thread model:** (1) Swing Event Dispatch Thread for drawing and input, (2) one game loop thread, (3) optional small thread pool for sound.
- **Game loop:** `ScheduledExecutorService` at a fixed rate (about 30 ticks per second).
- **Shared resources:** the command queue and the render snapshot. The model itself is changed only by the game thread.
- **Synchronization:** `ConcurrentLinkedQueue<Command>` for input; the loop publishes an immutable `RenderSnapshot` through a `volatile` reference so painting never reads data that is being changed.
- **Race condition addressed:** if Swing painted the live entity lists while the loop modified them, it could throw `ConcurrentModificationException` or draw half-updated state. The snapshot and queue prevent this (see Experiment 2).
- **Thread lifecycle:** loop thread starts when a stage starts, is paused/stopped on pause, win or loss, and is shut down on exit. TODO: describe exactly how you implemented it.

## 6.3. GUI

- Java Swing: `JFrame`, `JPanel` with `Graphics2D`, `CardLayout` to switch screens (TODO: confirm).
- Event handling: mouse listeners for cards, tiles and the charger click; key for pause.
- State management: `GameState` enum controls what is updated and drawn.
- Animation: tick counters drive sprite frame swapping, charger flashing and boss attack timers.
- Drawing: sprites loaded once as `BufferedImage` and drawn with `drawImage`; enemy purple tint applied once at load.

## 6.4. Networking

Not used.

## 6.5. Database

Not used.

## 6.6. Functional Programming

- Streams and lambdas to select entities, for example enemies in a row or inside the 3x3 explosion area (`filter`, `forEach`, `collect`). TODO: keep only what you use.

## 6.7. Meta-Programming

Not used.

---

# 7. Project Structure

```text
project/
├── source/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/game/
│       │   │   ├── Main.java
│       │   │   ├── core/      (GameModel, GameLoop, GameState, Command)
│       │   │   ├── entity/    (Defender, Enemy, Boss, Projectile, ...)
│       │   │   ├── stage/     (StageConfig, StageLoader, ProgressStore)
│       │   │   ├── ui/        (GamePanel, InputHandler, MainWindow)
│       │   │   └── audio/     (SoundPlayer)
│       │   └── resources/
│       │       ├── sprites/
│       │       ├── sounds/
│       │       └── stages/    (stage text files)
│       └── test/java/game/    (JUnit tests)
├── report/
│   └── GroupXX_FluSeasonDefense_Report.docx
└── README.md
```

---

# 8. Requirements

## Software

- Java version: JDK 17 or newer (TODO: confirm the version used in class)
- IDE: TODO (e.g. IntelliJ IDEA / Eclipse / VS Code)
- Build tool: Maven (TODO: change if you use another)
- Database: none
- Other dependencies: JUnit 5 (tests only)

## Hardware

No special hardware. A mouse and a screen of at least 1024x768.

---

# 9. Installation

```text
1. Install JDK 17 or newer and check:  java -version
2. Install Maven and check:            mvn -version
3. Copy or unzip the project folder.
4. Open a terminal in project/source
5. Build:                              mvn compile
```

TODO: replace these steps if you build differently, and test them on a clean computer.

---

# 10. Run

## Run Application

```text
cd project/source
mvn compile
java -cp target/classes game.Main
```

## Controls

- Click a unit card, then click a grid tile to place it.
- Click a credit icon to collect credits.
- Click the Broken Charger to detonate it early.
- Press `P` to pause (TODO: confirm key).

---

# 11. Testing

## Test Environment

- OS: TODO
- Java version: TODO
- Tools: JUnit 5, run with `mvn test`

## Test Cases

Fill in **Actual Result** and **Status** only after you run each test.

| ID   | Test Case | Input | Expected Result | Actual Result | Status |
| ---- | --------- | ----- | --------------- | ------------- | ------ |
| TC01 | Place unit with enough credits | 100 credits, place PC (cost 50) | PC on tile, credits = 50 | TODO | TODO |
| TC02 | Place unit without enough credits | 20 credits, place Year 1 (cost 100) | Placement rejected, credits unchanged | TODO | TODO |
| TC03 | Projectile hits enemy | Paper ball (20 dmg) overlaps enemy in same row | Enemy health reduced by 20, projectile removed | TODO | TODO |
| TC04 | Enemy reaches left edge | Enemy x passes the left boundary | Game state becomes LOST | TODO | TODO |
| TC05 | Charger in mine state | Charger at 5 s, enemy touches it | Enemy takes 600 damage, only that tile affected | TODO | TODO |
| TC06 | Charger in bomb state (click) | Charger at 10 s, player clicks it | 3x3 area enemies take 1800, plants unharmed | TODO | TODO |
| TC07 | Charger unstable timeout | Charger reaches 20 s | Explodes, enemies in 3x3 take 1800, plants in 3x3 take 400 | TODO | TODO |
| TC08 | Promote Year 1 to Year 2 | Place cap on a Year 1 student with enough credits | Student becomes Year 2, credits reduced by cap cost | TODO | TODO |
| TC09 | Boss phase change | Boss health drops below 60% | Boss phase changes to the next phase | TODO | TODO |
| TC10 | Command queue | Click command pushed from Swing thread | Applied by the loop thread at the next tick | TODO | TODO |
| TC11 | Load valid stage file | Well-formed stage file | `StageConfig` has the right waves and unlocked cards | TODO | TODO |
| TC12 | Load malformed stage file | File with a bad line | Error handled, message shown, game does not crash | TODO | TODO |
| TC13 | Save and load progress | Clear stage 1, restart game | Stage 2 is unlocked | TODO | TODO |
| TC14 | Weaboo attack odds | 10,000 attack rolls | Frequencies close to 40/30/20/10 percent | TODO | TODO |

---

# 12. Experimental Evaluation

TODO: run these and keep only the ones you really performed.

| ID | Experiment | Variable | Metric |
| -- | ---------- | -------- | ------ |
| E1 | Collision detection: check only entities in the same row vs check all pairs | Entity count (50, 200, 500) | Average tick time (ms) |
| E2 | Input handling: command queue vs changing the model directly from Swing | Number of rapid clicks during play, repeated runs | Number of exceptions or inconsistent states |
| E3 | Loop stability | 60 seconds of normal play | Average and maximum tick time, ticks per second |

## Metrics

- Tick time (ms), measured with `System.nanoTime()`.
- Ticks per second.
- Error count (exceptions, inconsistent states).

Test machine: TODO (CPU, RAM, OS, Java version).

---

# 13. Results

TODO: add measured results only. Do not estimate or invent numbers.

**E1: Collision detection**

| Entities | Row-only (ms) | All pairs (ms) |
| -------- | ------------- | -------------- |
| 50       | TODO          | TODO           |
| 200      | TODO          | TODO           |
| 500      | TODO          | TODO           |

**E2: Input handling**

| Approach | Runs | Errors observed |
| -------- | ---- | --------------- |
| Direct model changes from Swing | TODO | TODO |
| Command queue | TODO | TODO |

**E3: Loop stability**

| Average tick (ms) | Maximum tick (ms) | Ticks per second |
| ----------------- | ----------------- | ---------------- |
| TODO              | TODO              | TODO             |

---

# 14. Novelty & Contributions

## 14.1. Existing/Baseline

The baseline is a standard tower defense game (Plants vs Zombies style) where the game logic is often mixed with drawing code and stages are hard-coded.

## 14.2. Our Contributions

TODO: keep two or three that you really built and can explain.

1. A command-queue and snapshot threading model that keeps the Swing thread away from the game model, compared against direct model changes (E2).
2. Stages defined in text files and loaded at runtime, with error handling for bad files.
3. A collision approach limited to the same row, measured against all-pairs checking (E1).
4. State-based design for charger, boss and game screens (State pattern).

## 14.3. Technical Impact

- **Reliability:** no shared-state races between GUI and game logic.
- **Maintainability:** new units and stages are added without changing the loop.
- **Performance:** TODO (report only measured numbers from E1 and E3).
- **Usability:** TODO.

---

# 15. Code Review & Optimization

TODO: fill in during development.

- Problems found in code review:
- Refactoring performed:
- Duplication removed:
- Performance improvements:

---

# 16. Limitations

TODO: adjust to the final game. Likely points:

- Only one boss and a small number of stages.
- Game numbers (health, costs, timers) are hand-tuned, not balanced by playtesting.
- Sprites are simple and some are placeholders.
- No networking or multiplayer.

---

# 17. Future Work

- Year 3 and Year 4 caps and a third boss phase.
- Sound effects and music.
- More stages and enemy types.
- Leaderboard and save slots.

---

# 18. References

TODO: list everything you actually used.

- Oracle, Java SE documentation (Swing, `java.util.concurrent`, `java.nio.file`).
- JUnit 5 user guide.
- Course lecture materials.
- Sprites: TODO (state which were drawn by hand, which were made with AI tools, which tool, and any free assets with their license).
- AI assistance: TODO (describe what AI helped with and what you wrote and understood yourself).

---

# 19. Report

Final report:

```text
report/GroupXX_FluSeasonDefense_Report.docx
```

The report must be checked using Compilatio before submission (similarity 20% or less).

---

# 20. Final Checklist

* [ ] Project builds successfully.
* [ ] Project runs successfully.
* [ ] Source code is included.
* [ ] README is completed (no `TODO` left).
* [ ] Tests have been completed.
* [ ] Experimental results are included where appropriate.
* [ ] Novelty/Contribution is clearly identified.
* [ ] Report is completed.
* [ ] Compilatio has been checked.
* [ ] Similarity <= 20%.
* [ ] All group members understand the project.
* [ ] Final version has been submitted through the LMS.
