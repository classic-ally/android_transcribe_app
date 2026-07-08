package dev.notune.transcribe;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Live streaming preview preferences, stored as a small file in filesDir like
 * the other settings (readable from the :ime process without a content
 * provider). Holds the preview refresh interval in milliseconds.
 */
public final class PreviewPrefs {
    private static final String FILE_NAME = "preview_tick_ms";
    /** Default refresh interval; also the minimum the slider allows. */
    public static final int DEFAULT_TICK_MS = 300;
    public static final int MIN_TICK_MS = 150;
    public static final int MAX_TICK_MS = 1000;

    private PreviewPrefs() {}

    /** Preview refresh interval in ms, clamped to the supported range. */
    public static int getTickMs(Context ctx) {
        File f = new File(ctx.getFilesDir(), FILE_NAME);
        if (!f.exists()) return DEFAULT_TICK_MS;
        try {
            String s = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).trim();
            return clamp(Integer.parseInt(s));
        } catch (IOException | NumberFormatException e) {
            return DEFAULT_TICK_MS;
        }
    }

    public static void setTickMs(Context ctx, int ms) {
        File f = new File(ctx.getFilesDir(), FILE_NAME);
        try {
            Files.write(f.toPath(), String.valueOf(clamp(ms)).getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) { }
    }

    private static int clamp(int ms) {
        return Math.max(MIN_TICK_MS, Math.min(MAX_TICK_MS, ms));
    }
}
