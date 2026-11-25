package com.readyforjava.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/** User-configurable colors. Defaults: black background, light-gray text, blue headings. */
public final class Theme {
    public static final Map<String, String> PALETTE = new LinkedHashMap<>();
    public static final String[] SLOTS = {"background", "text", "heading"};

    static {
        String[][] colors = {
            {"black", "#000000"}, {"white", "#FFFFFF"}, {"silver", "#D4D4D4"}, {"gray", "#6A737D"},
            {"red", "#F44747"}, {"orange", "#D19A66"}, {"yellow", "#E5C07B"}, {"green", "#98C379"},
            {"cyan", "#56B6C2"}, {"blue", "#61AFEF"}, {"magenta", "#C678DD"},
            {"navy", "#0B1E3A"}, {"forest", "#0F2A1A"}, {"plum", "#2A1236"}
        };
        for (String[] c : colors) PALETTE.put(c[0], c[1]);
    }

    private static final Pattern HEX = Pattern.compile("#?[0-9a-fA-F]{6}");
    private static final Path FILE =
            Path.of(System.getProperty("user.home"), ".readyforjava", "config.properties");

    private final Map<String, String> values = new LinkedHashMap<>();

    private Theme() { reset(); }

    public String background() { return values.get("background"); }
    public String text() { return values.get("text"); }
    public String heading() { return values.get("heading"); }
    public String get(String slot) { return values.get(slot); }

    public void reset() {
        values.put("background", "#000000");
        values.put("text", "#D4D4D4");
        values.put("heading", "#61AFEF");
    }

    /** Sets a slot from a palette name or hex value. Returns false if the color is invalid. */
    public boolean set(String slot, String input) {
        String hex = resolve(input);
        if (hex == null || !values.containsKey(slot)) return false;
        values.put(slot, hex);
        return true;
    }

    public static String resolve(String input) {
        if (input == null) return null;
        String s = input.trim().toLowerCase();
        if (PALETTE.containsKey(s)) return PALETTE.get(s);
        if (HEX.matcher(s).matches()) return "#" + s.replace("#", "").toUpperCase();
        return null;
    }

    public static String fg(String hex) { return "\u001b[38;2;" + rgb(hex) + "m"; }
    public static String bg(String hex) { return "\u001b[48;2;" + rgb(hex) + "m"; }

    private static String rgb(String hex) {
        int v = Integer.parseInt(hex.substring(1), 16);
        return ((v >> 16) & 255) + ";" + ((v >> 8) & 255) + ";" + (v & 255);
    }

    public static Theme load() {
        Theme theme = new Theme();
        try {
            if (Files.exists(FILE)) {
                Properties p = new Properties();
                try (var r = Files.newBufferedReader(FILE)) { p.load(r); }
                for (String slot : SLOTS) theme.set(slot, p.getProperty(slot));
            }
        } catch (IOException ignored) { /* use defaults */ }
        return theme;
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Properties p = new Properties();
            values.forEach(p::setProperty);
            try (var w = Files.newBufferedWriter(FILE)) { p.store(w, "readyforjava theme"); }
        } catch (IOException ignored) { /* non-fatal */ }
    }
}
