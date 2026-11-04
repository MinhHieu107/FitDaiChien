package game.audio;

import javax.sound.sampled.*;

public class SoundTest {
    public static void main(String[] args) {
        // liệt kê thiết bị âm thanh
        System.out.println("=== Thiet bi am thanh Java nhin thay ===");
        Mixer.Info[] ds = AudioSystem.getMixerInfo();
        if (ds.length == 0) System.out.println("(KHONG co thiet bi nao -> Java khong phat duoc tieng tren may nay)");
        for (int i = 0; i < ds.length; i++) {
            boolean phatDuoc = AudioSystem.getMixer(ds[i]).isLineSupported(new Line.Info(Clip.class));
            System.out.printf("[%d] %s - %s%s%n", i, ds[i].getName(), ds[i].getDescription(),
                    phatDuoc ? "   <- phat duoc clip" : "");
        }

        //thử phát từng âm
        System.out.println();
        System.out.println("=== Thu phat tung am (hay lang nghe loa / tai nghe) ===");
        int loi = 0;
        for (Sfx.Id id : Sfx.Id.values()) {
            System.out.printf("%-15s %-34s ", id, Sfx.moTa(id));
            String ketQua = Sfx.thu(id);
            if (ketQua == null) System.out.println("-> OK");
            else { System.out.println("-> LOI: " + ketQua); loi++; }
        }

        //kết luận
        System.out.println();
        if (loi == 0) {
            System.out.println("Java bao tat ca deu phat OK. Neu ban van khong nghe thay gi thi kiem tra:");
            System.out.println("  - Am luong may / khong bi tat tieng; dung thiet bi phat mac dinh (loa hay tai nghe / Bluetooth / HDMI).");
            System.out.println("  - Dang chay game.audio.SoundMain chu khong phai game.Main (Main goc khong co tieng).");
            System.out.println("  - Trong game chua bam phim M (M = tat / bat tieng).");
        } else {
            System.out.println("Co " + loi + " am bi loi, hay gui nguyen phan chu o tren de minh xem tiep.");
        }
        System.exit(0);
    }
}
