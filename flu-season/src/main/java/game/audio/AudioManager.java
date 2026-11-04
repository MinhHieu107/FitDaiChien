
package game.audio;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quan ly nhac nen va hieu ung am thanh cua game.
 */
public final class AudioManager implements AutoCloseable {

    private static final String SOUND_DIR = "/sounds/";

    private static final String[] EFFECTS = {
            "ui_click", "unit_place", "attack",
            "enemy_hit", "victory", "defeat"
    };

    // Am luong BGM mac dinh: 30%
    private static final float DEFAULT_MUSIC_VOLUME = 0.15f;

    private final Map<String, Clip> clips = new ConcurrentHashMap<>();

    private Clip music;
    private volatile boolean closed;

    public AudioManager() {
        for (String name : EFFECTS) {
            Clip clip = loadClip(name + ".wav");
            if (clip != null) {
                clips.put(name, clip);
            }
        }

        music = loadClip("bgm_loop.wav");

        // Ap dung am luong ngay sau khi tai nhac nen
        setVolume(music, DEFAULT_MUSIC_VOLUME);
    }

    /**
     * Phat nhac nen lap vo han.
     */
    public synchronized void startMusic() {
        if (closed || music == null) return;

        if (!music.isRunning()) {
            music.setFramePosition(0);
            music.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    /**
     * Thay doi am luong nhac nen.
     * volume nam trong khoang 0.0 - 1.0.
     */
    public synchronized void setMusicVolume(float volume) {
        if (closed) return;
        setVolume(music, volume);
    }

    /**
     * Phat mot hieu ung am thanh.
     */
    public synchronized void playEffect(String name) {
        if (closed || name == null) return;

        Clip clip = clips.get(name);
        if (clip == null) return;

        if (clip.isRunning()) {
            clip.stop();
        }

        clip.setFramePosition(0);
        clip.start();
    }

    /**
     * Tai file am thanh tu resources/sounds.
     */
    private Clip loadClip(String fileName) {
        URL resource = AudioManager.class.getResource(
                SOUND_DIR + fileName
        );

        if (resource == null) {
            System.err.println(
                    "[AudioManager] Khong tim thay resource: "
                            + SOUND_DIR + fileName
            );
            return null;
        }

        try (AudioInputStream input =
                     AudioSystem.getAudioInputStream(resource)) {

            Clip clip = AudioSystem.getClip();
            clip.open(input);

            return clip;

        } catch (Exception e) {
            System.err.println(
                    "[AudioManager] Khong the tai am thanh "
                            + fileName + ": " + e.getMessage()
            );
            return null;
        }
    }

    /**
     * Dieu chinh am luong theo ti le tu 0.0 den 1.0.
     */
    private void setVolume(Clip clip, float volume) {
        if (clip == null) return;

        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }

        FloatControl gain = (FloatControl) clip.getControl(
                FloatControl.Type.MASTER_GAIN
        );

        volume = Math.max(0.0f, Math.min(1.0f, volume));

        float dB;

        if (volume <= 0.0f) {
            dB = gain.getMinimum();
        } else {
            dB = (float) (20 * Math.log10(volume));
        }

        gain.setValue(
                Math.max(
                        gain.getMinimum(),
                        Math.min(gain.getMaximum(), dB)
                )
        );
    }

    @Override
    public synchronized void close() {
        if (closed) return;

        closed = true;

        for (Clip clip : clips.values()) {
            clip.stop();
            clip.close();
        }

        clips.clear();

        if (music != null) {
            music.stop();
            music.close();
            music = null;
        }
    }
}
