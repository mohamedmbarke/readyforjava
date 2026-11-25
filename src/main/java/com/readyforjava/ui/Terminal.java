package com.readyforjava.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/** Themed console I/O. Colors never use a full reset, so the background stays painted. */
public final class Terminal {
    private static final String ESC = "\u001b[";

    private final Theme theme;
    private final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    public Terminal(Theme theme) {
        this.theme = theme;
        applyBase();
    }

    public Theme theme() { return theme; }

    private void applyBase() {
        System.out.print(Theme.bg(theme.background()) + Theme.fg(theme.text()));
    }

    /** Repaints the whole screen with the current theme. */
    public void clear() {
        applyBase();
        System.out.print(ESC + "2J" + ESC + "H");
        System.out.flush();
    }

    private String paint(String s, String hex) { return Theme.fg(hex) + s + Theme.fg(theme.text()); }

    public String heading(String s) { return paint(s, theme.heading()); }
    public String good(String s) { return paint(s, Theme.PALETTE.get("green")); }
    public String bad(String s) { return paint(s, Theme.PALETTE.get("red")); }
    public String dim(String s) { return paint(s, Theme.PALETTE.get("gray")); }
    public String link(String s) { return paint(s, Theme.PALETTE.get("cyan")); }

    public void println(String s) { System.out.println(s); }
    public void blank() { System.out.println(); }
    public void code(String line) { System.out.println(Highlighter.line(line, theme)); }

    /** Prompts for input; returns null at end of input. */
    public String ask(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        try {
            String line = in.readLine();
            return line == null ? null : line.trim();
        } catch (IOException e) {
            return null;
        }
    }

    public void pause() { ask(dim("  Press Enter to continue...")); }

    public void close() {
        System.out.print(ESC + "0m" + System.lineSeparator());
        System.out.flush();
    }
}
