package game;

import game.audio.AudioManager;
import game.core.*;
import game.stage.ProgressStore;
import game.ui.*;
import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AudioManager audio = new AudioManager();
            audio.startMusic();

            GamePanel panel = new GamePanel();
            GameModel model = new GameModel(new ProgressStore(Path.of("progress.txt")));
            model.setSoundHandler(audio::playEffect);

            AtomicReference<GameState> previousState = new AtomicReference<>(GameState.MENU);
            GameLoop loop = new GameLoop(model, snapshot -> {
                panel.publish(snapshot);
                GameState current = snapshot.state();
                GameState previous = previousState.getAndSet(current);
                if (current != previous) {
                    if (current == GameState.WON || current == GameState.ENDING) {
                        audio.playEffect("victory");
                    } else if (current == GameState.LOST) {
                        audio.playEffect("defeat");
                    }
                }
            });

            InputHandler in = new InputHandler(loop);
            panel.addMouseListener(in);
            panel.addMouseMotionListener(in);
            panel.addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) audio.playEffect("ui_click");
                }
            });
            InputHandler.installKeys(panel, loop);

            JFrame f = new JFrame("FIT Dai Chien");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setResizable(false);
            f.add(panel);
            f.pack();
            f.setLocationRelativeTo(null);
            f.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) {
                    loop.stop();
                    audio.close();
                }
            });
            // Nut Quit: model chi goi Runnable, khong biet gi ve Swing
            model.setQuitHandler(() -> SwingUtilities.invokeLater(
                    () -> f.dispatchEvent(new WindowEvent(f, WindowEvent.WINDOW_CLOSING))));
            f.setVisible(true);
            loop.start();
        });
    }
}
