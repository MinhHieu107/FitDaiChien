package game.audio;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


public final class Sfx {

    /** Danh sách các âm thanh */
    public enum Id {
        CLICK(30), CARD_SELECT(30), BUZZER(150), PLACE(60),
        TOKEN_SPAWN(60), CREDIT(40),
        THROW(45), HIT(35), DEATH(90), CHOMP(220),
        EXPLOSION(150), ALERT(500), LOSE(0),
        TYPE(40),                 // tiếng gõ chữ nhẹ khi đoạn giới thiệu (Intro) hiện từng ký tự
        DIG(300),                 // con gián kéo nhân vật (remove)
        ZOMBIES_COMING(1000),     // con zombie đầu tiên của màn chơi xuất hiện
        BRAINS(12000),            // zombie bắt đầu tấn công nhân vật (tối đa 12 giây phát lại 1 lần)
        BOSS(3000);               // boss (Prof. Deadline) xuất hiện

        final int minGapMs;
        Id(int minGapMs) { this.minGapMs = minGapMs; }
    }

    private record Sample(AudioFormat format, byte[] pcm) {}

    // Tần số lấy mẫu 44100 Hz, 16 bit
    private static final float RATE = 44100f;
    private static final AudioFormat FORMAT = new AudioFormat(RATE, 16, 1, true, false);

    private static final int MAX_VOICES = 24;

    // Kho chứa các âm đã nạp sẵn, thời điểm phát gần nhất của từng âm, và số âm đang phát
    private static final Map<Id, Sample> SAMPLES = new EnumMap<>(Id.class);
    private static final long[] lastPlayed = new long[Id.values().length];
    private static final AtomicInteger voices = new AtomicInteger();

    // Những âm đang dùng file wav của bạn (các âm còn lại là bản tự tạo bằng code) - để SoundTest báo cáo
    private static final Set<Id> TU_FILE_WAV = EnumSet.noneOf(Id.class);

    // Đếm số lần phát lỗi để chỉ in thông báo vài lần đầu, tránh tràn màn hình console
    private static int soLanLoi = 0;

    // Luồng riêng để phát nhạc
    private static final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "sfx");
        t.setDaemon(true);
        return t;
    });

    // muted = đang tắt tiếng (phím M)
    private static volatile boolean muted = false;
    private static volatile boolean available = true;
    private static volatile float masterGain = 0.8f;

    private Sfx() {}


    public static void init() { }

    public static void play(Id id) {
        // Đang tắt tiếng hoặc máy không có loa -> không làm gì
        if (muted || !available) return;

        long now = System.nanoTime() / 1_000_000L;
        synchronized (lastPlayed) {
            if (now - lastPlayed[id.ordinal()] < id.minGapMs) return;
            lastPlayed[id.ordinal()] = now;
        }

        Sample s = SAMPLES.get(id);
        if (s == null || voices.get() >= MAX_VOICES) return;

        worker.execute(() -> start(s));
    }

    // Bật / tắt tiếng
    public static void setMuted(boolean m) { muted = m; }
    public static void toggleMute() { muted = !muted; }
    public static boolean isMuted() { return muted; }
    public static void setVolume(float v) { masterGain = Math.max(0f, Math.min(1f, v)); }

    private static void start(Sample s) {
        // Bước 1: xin hệ điều hành một "kênh phát". Lỗi ở đây = máy không có thiết bị âm thanh phù hợp
        Clip clip;
        try {
            clip = AudioSystem.getClip();
        } catch (Exception e) {
            available = false;      // tắt hẳn để khỏi báo lỗi liên tục
            System.err.println("[Sfx] Khong tim thay thiet bi phat am thanh, game se chay im lang: " + e);
            return;
        }


        try {
            clip.open(s.format(), s.pcm(), 0, s.pcm().length);
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl g = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float db = 20f * (float) Math.log10(Math.max(0.0001, masterGain));
                g.setValue(Math.max(g.getMinimum(), Math.min(g.getMaximum(), db)));
            }

            voices.incrementAndGet();
            clip.addLineListener(ev -> {
                if (ev.getType() == LineEvent.Type.STOP) { clip.close(); voices.decrementAndGet(); }
            });
            clip.start();
        } catch (Exception e) {
            clip.close();
            if (soLanLoi++ < 3) System.err.println("[Sfx] Khong phat duoc mot am thanh (" + s.format() + "): " + e);
        }
    }

    static String thu(Id id) {
        Sample s = SAMPLES.get(id);
        if (s == null) return "khong co du lieu am thanh (chua co file wav va cung khong tu tao duoc)";
        try {
            Clip clip = AudioSystem.getClip();
            clip.open(s.format(), s.pcm(), 0, s.pcm().length);
            CountDownLatch xong = new CountDownLatch(1);
            clip.addLineListener(ev -> { if (ev.getType() == LineEvent.Type.STOP) xong.countDown(); });
            clip.start();
            xong.await(2, TimeUnit.SECONDS);
            clip.stop();
            clip.close();
            return null;
        } catch (Exception e) {
            return e.toString();
        }
    }


    static String moTa(Id id) {
        Sample s = SAMPLES.get(id);
        if (s == null) return "(khong co du lieu)";
        double giay = s.pcm().length / (double) (s.format().getFrameSize() * s.format().getFrameRate());
        return (TU_FILE_WAV.contains(id) ? "file wav cua ban" : "tu tao bang code") + String.format(", %.1f giay", giay);
    }


    static {
        Random rng = new Random(7);
        Arrays.fill(lastPlayed, Long.MIN_VALUE / 2);
        for (Id id : Id.values()) {
            Sample s = loadWav(id);
            if (s != null) TU_FILE_WAV.add(id);
            if (s == null) s = synth(id, rng);
            if (s != null) SAMPLES.put(id, s);
        }
    }

    private static Sample synth(Id id, Random rng) {
        switch (id) {
            case CREDIT -> {
                // Tiếng "ting-ting" sáng, đi lên khi nhặt xu: 4 nốt E6 G#6 B6 E7 nối nhau
                float[] o = buf(0.6);
                double[] f = {1318.5, 1661.2, 1975.5, 2637.0};
                double[] g = {0.7, 0.8, 0.9, 1.0};
                for (int i = 0; i < f.length; i++)
                    layer(o, norm(sweep(f[i], f[i], 0.4, 8, 1, 0.3, 0.12)), 0.055 * i, g[i]);
                return pcm(o, 0.7);
            }
            case TOKEN_SPAWN -> {
                // Tiếng "bloop" nhẹ khi PC đẻ ra 1 xu
                return pcm(sweep(380, 760, 0.14, 22, 1, 0.2), 0.4);
            }
            case PLACE -> {
                // Tiếng "bịch" trầm + sột soạt khi đặt nhân vật xuống
                float[] o = buf(0.2);
                layer(o, norm(sweep(150, 60, 0.18, 16, 1)), 0, 1.0);
                layer(o, norm(noise(rng, 0.14, 0.12, 0.35, 22)), 0, 0.5);
                return pcm(o, 0.6);
            }
            case CARD_SELECT -> {
                // Tiếng "tách" ngắn khi chọn thẻ
                return pcm(sweep(700, 1050, 0.08, 26, 1, 0.3), 0.35);
            }
            case BUZZER -> {
                // Tiếng "bzzt" báo lỗi (không đặt được / không đủ tiền)
                return pcm(sweep(130, 124, 0.22, 5, 1, 0, 0.33, 0, 0.2, 0, 0.14), 0.4);
            }
            case CLICK -> {
                // Tiếng "tạch" khi bấm nút menu
                float[] o = buf(0.05);
                layer(o, norm(noise(rng, 0.02, 0.6, 0.6, 150)), 0, 0.5);
                layer(o, norm(sweep(1200, 900, 0.045, 60, 1)), 0, 1.0);
                return pcm(o, 0.3);
            }
            case THROW -> {
                // Tiếng "vút" ngắn khi ném / bắn
                float[] n = noise(rng, 0.13, 0.04, 0.4, 0);
                window(n, 1.5);
                return pcm(n, 0.35);
            }
            case HIT -> {
                // Tiếng "bẹp" khi đạn trúng
                float[] o = buf(0.16);
                layer(o, norm(noise(rng, 0.14, 0.45, 0.12, 38)), 0, 1.0);
                layer(o, norm(sweep(240, 80, 0.1, 30, 1, 0.4)), 0, 0.8);
                return pcm(o, 0.7);
            }
            case DEATH -> {
                // Tiếng rên "grừ..."
                int n = (int) (0.55 * RATE);
                float[] o = new float[n];
                double phase = 0, lp = 0;
                for (int i = 0; i < n; i++) {
                    double t = i / RATE, k = (double) i / n;
                    double f = (190 - 120 * k) * (1 + 0.05 * Math.sin(2 * Math.PI * 7 * t));
                    phase += 2 * Math.PI * f / RATE;
                    double v = 0;
                    for (int h = 1; h <= 5; h++) v += Math.sin(phase * h) / h;
                    lp += 0.2 * (v - lp);
                    o[i] = (float) lp;
                }
                window(o, 0.7);
                float[] growl = noise(rng, 0.55, 0.08, 0.05, 0);
                window(growl, 1.0);
                layer(o, norm(growl), 0, 0.3);
                return pcm(o, 0.55);
            }
            case CHOMP -> {
                // Tiếng nhai
                float[] o = buf(0.26);
                for (int i = 0; i < 2; i++) {
                    layer(o, norm(sweep(130, 55, 0.09, 30, 1, 0.5)), 0.11 * i, 1.0);
                    layer(o, norm(noise(rng, 0.07, 0.2, 0.05, 45)), 0.11 * i, 0.6);
                }
                return pcm(o, 0.6);
            }
            case EXPLOSION -> {
                // Tiếng nổ "bùm"
                float[] o = buf(0.95);
                layer(o, norm(noise(rng, 0.9, 0.18, 0.02, 4.5)), 0, 1.0);
                layer(o, norm(sweep(100, 28, 0.7, 5, 1, 0.5)), 0, 1.0);
                layer(o, norm(noise(rng, 0.15, 0.6, 0.3, 25)), 0, 0.6);
                return pcm(o, 0.9);
            }
            case ALERT -> {
                // Tiếng còi trầm báo động
                float[] o = buf(1.1);
                float[] a = sweep(98, 98, 0.4, 0, 1, 0.6, 0.5, 0.4, 0.3, 0.2);
                float[] b = sweep(130, 130, 0.65, 0, 1, 0.6, 0.5, 0.4, 0.3, 0.2);
                window(a, 0.6);
                window(b, 0.6);
                layer(o, norm(a), 0, 0.8);
                layer(o, norm(b), 0.42, 1.0);
                return pcm(o, 0.6);
            }
            case TYPE -> {
                // Tiếng "tách" nhỏ
                float[] o = buf(0.03);
                layer(o, norm(noise(rng, 0.014, 0.5, 0.5, 220)), 0, 0.6);
                layer(o, norm(sweep(1800, 1400, 0.02, 150, 1)), 0, 0.4);
                return pcm(o, 0.16);
            }
            case DIG -> {
                // Con gián remove
                float[] o = buf(0.7);
                layer(o, norm(noise(rng, 0.6, 0.5, 0.15, 3.0)), 0, 0.35);
                double t = 0.02;
                while (t < 0.6) {
                    layer(o, norm(noise(rng, 0.010, 0.6, 0.6, 250)), t, 0.6 + 0.4 * rng.nextDouble());
                    layer(o, norm(sweep(2600 + 800 * rng.nextDouble(), 1800, 0.012, 200, 1)), t, 0.3);
                    t += 0.025 + 0.035 * rng.nextDouble() * (t / 0.3);
                }
                window(o, 0.5);
                return pcm(o, 0.6);
            }
            case LOSE -> {
                // Nhạc thua
                float[] o = buf(2.0);
                double[] f = {440, 349.2, 293.7, 220};
                double[] d = {0.4, 0.4, 0.4, 1.0};
                for (int i = 0; i < f.length; i++)
                    layer(o, norm(sweep(f[i], f[i] * 0.98, d[i], 3, 1, 0.5, 0.25)), 0.28 * i, 1.0);
                return pcm(o, 0.6);
            }
        }
        return null;
    }


    private static float[] buf(double seconds) { return new float[(int) (seconds * RATE)]; }

    private static float[] sweep(double f0, double f1, double dur, double decay, double... harm) {
        float[] o = buf(dur);
        double phase = 0;
        for (int i = 0; i < o.length; i++) {
            double t = i / RATE, k = t / dur;
            double f = f0 * Math.pow(f1 / f0, k);
            phase += 2 * Math.PI * f / RATE;
            double v = 0;
            for (int h = 0; h < harm.length; h++) v += harm[h] * Math.sin(phase * (h + 1));
            o[i] = (float) (v * Math.min(1.0, t / 0.004) * Math.exp(-decay * t));
        }
        return o;
    }


    private static float[] noise(Random r, double dur, double a0, double a1, double decay) {
        float[] o = buf(dur);
        double y = 0;
        for (int i = 0; i < o.length; i++) {
            double t = i / RATE, k = (double) i / o.length;
            double a = a0 + (a1 - a0) * k;
            y += a * ((r.nextDouble() * 2 - 1) - y);
            o[i] = (float) (y * Math.min(1.0, t / 0.002) * Math.exp(-decay * t));
        }
        return o;
    }


    private static void window(float[] a, double power) {
        for (int i = 0; i < a.length; i++)
            a[i] *= (float) Math.pow(Math.max(0.0, Math.sin(Math.PI * i / (a.length - 1.0))), power);
    }

    private static float[] norm(float[] a) {
        float max = 0;
        for (float v : a) max = Math.max(max, Math.abs(v));
        if (max > 0) for (int i = 0; i < a.length; i++) a[i] /= max;
        return a;
    }

    private static void layer(float[] dest, float[] src, double atSec, double gain) {
        int off = (int) (atSec * RATE);
        for (int i = 0; i < src.length && off + i < dest.length; i++) dest[off + i] += (float) (src[i] * gain);
    }

    private static Sample pcm(float[] f, double peak) {
        float max = 0;
        for (int i = 0; i < f.length; i++) {
            if (Float.isNaN(f[i]) || Float.isInfinite(f[i])) f[i] = 0f;
            max = Math.max(max, Math.abs(f[i]));
        }
        float g = max > 0 ? (float) (peak / max) : 1f;

        int fade = (int) (0.005 * RATE);
        byte[] b = new byte[f.length * 2];
        for (int i = 0; i < f.length; i++) {
            float v = f[i] * g;
            int rem = f.length - 1 - i;
            if (rem < fade) v *= rem / (float) fade;
            short s = (short) Math.round(Math.max(-1f, Math.min(1f, v)) * 32767);
            b[2 * i] = (byte) s;
            b[2 * i + 1] = (byte) (s >> 8);
        }
        return new Sample(FORMAT, b);
    }

    private static Sample loadWav(Id id) {
        String path = "sounds/" + id.name().toLowerCase() + ".wav";
        try (InputStream in = Sfx.class.getResourceAsStream(path)) {
            if (in == null) return null;
            AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(in));
            return new Sample(ais.getFormat(), ais.readAllBytes());
        } catch (Exception e) {
            return null;
        }
    }


    public static void main(String[] args) throws Exception {
        Path dir = Path.of(args.length > 0 ? args[0] : "sfx-out");
        Files.createDirectories(dir);
        for (Id id : Id.values()) {
            Sample s = SAMPLES.get(id);
            if (s == null) continue;
            AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(s.pcm()), s.format(),
                    s.pcm().length / s.format().getFrameSize());
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, dir.resolve(id.name().toLowerCase() + ".wav").toFile());
        }
        System.out.println("Da xuat file vao: " + dir.toAbsolutePath());
        System.exit(0);
    }
}
