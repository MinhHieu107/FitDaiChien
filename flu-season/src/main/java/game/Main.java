package game;

import game.core.*;
import game.ui.*;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GamePanel panel = new GamePanel();
            GameModel model = new GameModel();
            GameLoop loop = new GameLoop(model, panel::publish);
            InputHandler in = new InputHandler(loop);
            panel.addMouseMotionListener(in);
            panel.addMouseListener(in);

            JFrame f = new JFrame("Flu Season Defense");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setResizable(false);
            f.add(panel); f.pack(); f.setLocationRelativeTo(null);
            f.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override public void windowClosing(java.awt.event.WindowEvent e) { loop.stop(); }
            });
            f.setVisible(true);
            loop.start();
        });
    }
}