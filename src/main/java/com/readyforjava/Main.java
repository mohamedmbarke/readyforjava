package com.readyforjava;

import com.readyforjava.content.CurriculumLoader;
import com.readyforjava.progress.ProgressTracker;
import com.readyforjava.ui.Terminal;
import com.readyforjava.ui.Theme;

import java.io.IOException;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        Terminal terminal = new Terminal(Theme.load());
        try {
            new App(terminal, CurriculumLoader.load(), new ProgressTracker()).run();
        } catch (IOException e) {
            System.err.println("Could not start readyforjava: " + e.getMessage());
        } finally {
            terminal.close();
        }
    }
}
