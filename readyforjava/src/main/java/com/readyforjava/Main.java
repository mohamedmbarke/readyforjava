package com.readyforjava;

import com.readyforjava.content.CurriculumLoader;
import com.readyforjava.model.Curriculum;
import com.readyforjava.progress.ProgressTracker;
import com.readyforjava.ui.Terminal;
import com.readyforjava.ui.Theme;
import com.readyforjava.web.WebServer;

import java.io.IOException;

/**
 * Usage:
 *   java -jar readyforjava.jar [web] [port]   website at http://localhost:8080 (default)
 *   java -jar readyforjava.jar terminal       terminal interface
 */
public final class Main {
    private Main() {}

    public static void main(String[] args) {
        boolean terminal = args.length > 0 && args[0].equalsIgnoreCase("terminal");
        try {
            Curriculum curriculum = CurriculumLoader.load();
            if (terminal) {
                Terminal t = new Terminal(Theme.load());
                try { new App(t, curriculum, new ProgressTracker()).run(); } finally { t.close(); }
            } else {
                int port = args.length > 1 ? Integer.parseInt(args[1]) : 8080;
                new WebServer(curriculum).start(port);
                System.out.println("readyforjava is running: http://localhost:" + port + "  (Ctrl+C to stop)");
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Could not start readyforjava: " + e.getMessage());
        }
    }
}