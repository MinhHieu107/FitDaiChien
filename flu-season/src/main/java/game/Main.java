package game;

import game.core.*;
import game.stage.ProgressStore;
import game.ui.*;
import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GamePanel panel = new GamePanel();
            GameModel model = new GameModel(new ProgressStore(Path.of("progress.txt")));
            GameLoop loop = new GameLoop(model, panel::publish);

            InputHandler in = new InputHandler(loop);
            panel.addMouseListener(in);
            panel.addMouseMotionListener(in);
            InputHandler.installKeys(panel, loop);

            JFrame f = new JFrame("Flu Season Defense");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setResizable(false);
            f.add(panel);
            f.pack();
            f.setLocationRelativeTo(null);
            f.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) { loop.stop(); }
            });
            // Nut Quit: model chi goi Runnable, khong biet gi ve Swing
            model.setQuitHandler(() -> SwingUtilities.invokeLater(
                    () -> f.dispatchEvent(new WindowEvent(f, WindowEvent.WINDOW_CLOSING))));
            f.setVisible(true);
            loop.start();
        });
    }
}
