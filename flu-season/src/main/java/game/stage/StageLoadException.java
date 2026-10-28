package game.stage;

/** Loi doc stage: thong diep da du thong tin de hien thi cho nguoi choi. */
public class StageLoadException extends Exception {
    public StageLoadException(String message) { super(message); }
    public StageLoadException(String message, Throwable cause) { super(message, cause); }
}
