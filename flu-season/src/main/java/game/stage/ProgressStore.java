package game.stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Luu stage cao nhat da mo khoa (0..3) trong 1 file text chi co 1 so nguyen. */
public class ProgressStore {
    public static final int MAX_STAGE = 3;
    private final Path file;

    public ProgressStore(Path file) { this.file = file; }

    /** File thieu, hong hoac so ngoai 0..3 -> coi nhu 0. Khong bao gio nem loi. */
    public int load() {
        try {
            int v = Integer.parseInt(Files.readString(file).strip());
            return (v >= 0 && v <= MAX_STAGE) ? v : 0;
        } catch (IOException | NumberFormatException e) {
            return 0;
        }
    }

    public void save(int unlocked) {
        try {
            Files.writeString(file, String.valueOf(unlocked));
        } catch (IOException e) {
            System.err.println("Cannot save progress to " + file + ": " + e.getMessage());   // game van choi tiep
        }
    }
}
