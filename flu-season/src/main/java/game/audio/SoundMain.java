package game.audio;

import game.core.*;
import game.stage.ProgressStore;
import game.ui.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;

public class SoundMain {
    public static void main(String[] args) {
        // Nạp sẵn toàn bộ âm thanh khi mở game
        Sfx.init();

        SwingUtilities.invokeLater(() -> {
            GamePanel panel = new GamePanel();
            // Khác Main gốc: dùng SoundGameModel
            GameModel model = new SoundGameModel(new ProgressStore(Path.of("progress.txt")));
            GameLoop loop = new GameLoop(model, panel::publish);

            InputHandler in = new InputHandler(loop);
            panel.addMouseListener(in);
            panel.addMouseMotionListener(in);
            InputHandler.installKeys(panel, loop);

            // Khác Main gốc: bấm M để tắt / bật toàn bộ âm thanh
            panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0), "mute");
            panel.getActionMap().put("mute", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { Sfx.toggleMute(); }
            });

            JFrame f = new JFrame("Fit Dai Chien");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setResizable(false);
            f.add(panel);
            f.pack();
            f.setLocationRelativeTo(null);
            f.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) { loop.stop(); }
            });
            model.setQuitHandler(() -> SwingUtilities.invokeLater(
                    () -> f.dispatchEvent(new WindowEvent(f, WindowEvent.WINDOW_CLOSING))));
            f.setVisible(true);
            loop.start();
        });
    }
}
