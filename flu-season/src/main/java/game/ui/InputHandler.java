package game.ui;

import game.core.*;
import javax.swing.*;
import java.awt.event.*;

public class InputHandler extends MouseAdapter {
    private final GameLoop loop;
    public InputHandler(GameLoop loop) { this.loop = loop; }

    @Override public void mouseMoved(MouseEvent e)   { hover(e); }
    @Override public void mouseDragged(MouseEvent e) { hover(e); }
    @Override public void mouseExited(MouseEvent e)  { loop.submit(m -> m.setMouse(-1, -1)); }

    private void hover(MouseEvent e) {
        int x = e.getX(), y = e.getY();
        loop.submit(m -> m.setMouse(x, y));
    }

    @Override public void mousePressed(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) return;
        int x = e.getX(), y = e.getY();
        loop.submit(m -> m.handleClick(x, y));      // GUI khong bao gio sua model truc tiep
    }

    /** Esc = huy the dang chon, P = tam dung / tiep tuc. */
    public static void installKeys(JComponent c, GameLoop loop) {
        bind(c, KeyEvent.VK_ESCAPE, "cancel", () -> loop.submit(GameModel::cancelSelection));
        bind(c, KeyEvent.VK_P, "pause", () -> loop.submit(GameModel::togglePause));
    }

    private static void bind(JComponent c, int key, String name, Runnable action) {
        c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key, 0), name);
        c.getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }
}
