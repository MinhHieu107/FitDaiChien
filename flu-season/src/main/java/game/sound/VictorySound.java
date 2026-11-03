package game.sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/** Nhanh nhac chien thang ngan duoc tong hop bang Java, khong can them file audio. */
public class VictorySound {
    private static final int SAMPLE_RATE = 22050;
    private static final double[] NOTES = { 523.25, 659.25, 783.99, 1046.50, 783.99, 1046.50 };
    private VictorySound() {}

    public static void play() {
        Thread soundThread = new Thread(() -> {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
                line.open(format, 4096);
                line.start();
                for (int n = 0; n < NOTES.length; n++) {
                    int duration = n == 3 || n == 5 ? 260 : 170;
                    int samples = SAMPLE_RATE * duration / 1000;
                    byte[] buffer = new byte[samples * 2];
                    for (int i = 0; i < samples; i++) {
                        double t = (double) i / SAMPLE_RATE;
                        double attack = Math.min(1.0, i / 180.0);
                        double decay = Math.exp(-4.2 * i / samples);
                        double fundamental = Math.sin(2 * Math.PI * NOTES[n] * t);
                        double harmonic = 0.32 * Math.sin(2 * Math.PI * NOTES[n] * 2.01 * t);
                        short value = (short) (Math.max(-1, Math.min(1,
                                (fundamental + harmonic) * attack * decay * 0.42)) * Short.MAX_VALUE);
                        buffer[i * 2] = (byte) (value & 0xff);
                        buffer[i * 2 + 1] = (byte) ((value >>> 8) & 0xff);
                    }
                    line.write(buffer, 0, buffer.length);
                    if (n != NOTES.length - 1) Thread.sleep(28);
                }
                line.drain();
                line.stop();
            } catch (Exception ignored) {
                // Audio device may be unavailable; gameplay must still continue normally.
            }
        }, "victory-sound");
        soundThread.setDaemon(true);
        soundThread.start();
    }
}
