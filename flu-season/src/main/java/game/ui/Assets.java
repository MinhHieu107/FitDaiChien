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

    private static final Map<String, BufferedImage> SHADOWS = new ConcurrentHashMap<>();

    /**
     * Chi phan bong (pixel ban trong suot) cua 1 sprite dung yen. Cac sheet hoat anh khong co bong,
     * nen khi doi tu anh dung yen sang hoat anh thi bong bi nhap nhay => ve bong rieng duoi hoat anh.
     */
    public static BufferedImage shadow(String spriteName) {
        return SHADOWS.computeIfAbsent(spriteName, n -> {
            BufferedImage src = get(n);
            BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < src.getHeight(); y++)
                for (int x = 0; x < src.getWidth(); x++) {
                    int argb = src.getRGB(x, y), a = argb >>> 24;
                    if (a > 0 && a < 255) out.setRGB(x, y, argb);
                }
            return out;
        });
    }

    /** Ve 1 o (frame, row) cua sheet/atlas, co gian ra (dw x dh). Frame va row tinh tu 0. */
    public static void drawFrame(Graphics2D g, BufferedImage sheet, int frame, int row,
                                 int fw, int fh, int x, int y, int dw, int dh) {
        int sx = frame * fw, sy = row * fh;
        g.drawImage(sheet, x, y, x + dw, y + dh, sx, sy, sx + fw, sy + fh, null);
    }
}
