package game.ui;

import game.core.*;
import javax.swing.*;
import java.awt.event.*;

public class InputHandler extends MouseAdapter {
    private final GameLoop loop;
    public InputHandler(GameLoop loop) { this.loop = loop; }
    @Override public void mouseMoved(MouseEvent e)   { hover(e); }
    @Override public void mouseDragged(MouseEvent e) { hover(e); }
    @Override public void mouseExited(MouseEvent e)  { loop.submit(m -> m.setHover(-1, -1)); }

    private void hover(MouseEvent e) {
        int[] t = GameModel.tileAt(e.getX(), e.getY());
        int r = t == null ? -1 : t[0], c = t == null ? -1 : t[1];
        loop.submit(m -> m.setHover(r, c));
    }

    @Override public void mousePressed(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) return;
        int x = e.getX(), y = e.getY();
        loop.submit(m -> m.handleClick(x, y));      // GUI khong bao gio sua model truc tiep
    }

    /** Phim Esc = huy the dang chon. */
    public static void installKeys(JComponent c, GameLoop loop) {
        c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        c.getActionMap().put("cancel", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { loop.submit(GameModel::cancelSelection); }
        });
    }
}
