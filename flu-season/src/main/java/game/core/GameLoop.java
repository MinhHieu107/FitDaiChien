package game.core;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class GameLoop {
    private final GameModel model;
    private final Queue<Command> queue = new ConcurrentLinkedQueue<>();
    private final Consumer<RenderSnapshot> publisher;
    private final ScheduledExecutorService exec =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "game-loop"); t.setDaemon(true); return t;
            });

    public GameLoop(GameModel model, Consumer<RenderSnapshot> publisher) {
        this.model = model; this.publisher = publisher;
    }

    public void submit(Command c) { queue.add(c); }

    public void start() {
        long period = 1_000_000_000L / Constants.TPS;
        exec.scheduleAtFixedRate(this::tick, 0, period, TimeUnit.NANOSECONDS);
    }

    public void stop() { exec.shutdownNow(); }

    private void tick() {
        try {
            Command c;
            while ((c = queue.poll()) != null) c.apply(model);   // bước 1 của tick
            model.update();
            publisher.accept(model.snapshot());                   // bước 10 của tick
        } catch (Throwable t) { t.printStackTrace(); }  // nếu không bắt, scheduler sẽ im lặng dừng hẳn
    }
}