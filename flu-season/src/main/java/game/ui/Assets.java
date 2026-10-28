package game.ui;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Load moi PNG dung 1 lan, cache lai. Ten file tinh tu thu muc resources/sprites/. */
public final class Assets {
    private static final Map<String, BufferedImage> CACHE = new ConcurrentHashMap<>();
    private Assets() {}

    public static BufferedImage get(String name) {
        return CACHE.computeIfAbsent(name, Assets::load);
    }

    private static BufferedImage load(String name) {
        String path = "/sprites/" + name;
        try (InputStream in = Assets.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Khong tim thay sprite: " + path);
            return ImageIO.read(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Ve 1 o (frame, row) cua sheet/atlas, co gian ra (dw x dh). Frame va row tinh tu 0. */
    public static void drawFrame(Graphics2D g, BufferedImage sheet, int frame, int row,
                                 int fw, int fh, int x, int y, int dw, int dh) {
        int sx = frame * fw, sy = row * fh;
        g.drawImage(sheet, x, y, x + dw, y + dh, sx, sy, sx + fw, sy + fh, null);
    }
}
