package com.readyforjava;

import com.readyforjava.model.*;
import com.readyforjava.progress.ProgressTracker;
import com.readyforjava.ui.Terminal;
import com.readyforjava.ui.Theme;

import java.util.List;

/** Menu-driven screens: learn, practice, progress, settings. */
public final class App {
    private final Terminal t;
    private final Curriculum curriculum;
    private final ProgressTracker progress;

    public App(Terminal t, Curriculum curriculum, ProgressTracker progress) {
        this.t = t;
        this.curriculum = curriculum;
        this.progress = progress;
    }

    public void run() {
        while (true) {
            t.clear();
            t.println(t.heading("  ╔═══════════════════════════╗"));
            t.println(t.heading("  ║        readyforjava       ║"));
            t.println(t.heading("  ╚═══════════════════════════╝"));
            t.println("  Readiness: " + bar(progress.percent(curriculum)) + " goal: "
                    + ProgressTracker.GOAL_PERCENT + "%");
            t.blank();
            t.println("  1) Learn a topic");
            t.println("  2) Practice all unsolved exercises");
            t.println("  3) Progress dashboard");
            t.println("  4) Settings (colors)");
            t.println("  5) Quit");
            t.blank();
            String choice = t.ask("  > ");
            if (choice == null || choice.equals("5")) return;
            switch (choice) {
                case "1" -> learnMenu();
                case "2" -> practiceAll();
                case "3" -> dashboard();
                case "4" -> settings();
                default -> { }
            }
        }
    }

    // ---------- Learn ----------

    private void learnMenu() {
        while (true) {
            t.clear();
            t.println(t.heading("  Topics"));
            t.blank();
            List<Topic> topics = curriculum.topics();
            for (int i = 0; i < topics.size(); i++) {
                Topic topic = topics.get(i);
                int done = progress.doneIn(topic), total = topic.exercises().size();
                String mark = done == total ? t.good("✔") : " ";
                t.println(String.format("  %2d) %s %-32s %d/%d", i + 1, mark, topic.title(), done, total));
            }
            t.blank();
            String in = t.ask("  Topic number (b = back) > ");
            if (in == null || in.equalsIgnoreCase("b")) return;
            try {
                int n = Integer.parseInt(in);
                if (n >= 1 && n <= topics.size()) showTopic(topics.get(n - 1));
            } catch (NumberFormatException ignored) { }
        }
    }

    private void showTopic(Topic topic) {
        t.clear();
        t.println(t.heading("  " + topic.title()));
        t.blank();
        for (String line : topic.explanation()) {
            if (line.startsWith("    ")) t.code("  " + line);
            else t.println("  " + line);
        }
        t.blank();
        t.println(t.heading("  Keep learning (external tutorials)"));
        for (Link l : topic.links()) t.println("   • " + l.title() + "  " + t.link(l.url()));
        t.blank();
        String in = t.ask("  p = practice this topic, Enter = back > ");
        if (in != null && in.equalsIgnoreCase("p")) practice(topic.title(), topic.exercises());
    }

    // ---------- Practice ----------

    private void practiceAll() {
        List<Exercise> todo = curriculum.topics().stream()
                .flatMap(tp -> tp.exercises().stream())
                .filter(e -> !progress.isDone(e.id())).toList();
        practice("All unsolved exercises", todo);
    }

    private void practice(String title, List<Exercise> all) {
        List<Exercise> todo = all.stream().filter(e -> !progress.isDone(e.id())).toList();
        t.clear();
        t.println(t.heading("  Practice: " + title));
        if (todo.isEmpty()) {
            t.println(t.good("  Everything here is already solved. Nice work!"));
            t.pause();
            return;
        }
        for (int i = 0; i < todo.size(); i++) {
            if (!ask(todo.get(i), i + 1, todo.size())) return;
        }
        t.blank();
        t.println(t.good("  Set complete! Readiness is now " + progress.percent(curriculum) + "%."));
        t.pause();
    }

    /** Returns false if the learner quit. */
    private boolean ask(Exercise e, int n, int total) {
        t.blank();
        t.println(t.heading("  Exercise " + n + "/" + total));
        String[] lines = e.prompt().split("\n");
        t.println("  " + lines[0]);
        for (int i = 1; i < lines.length; i++) t.code("      " + lines[i]);
        for (String option : e.options()) t.println("      " + option);
        while (true) {
            String a = t.ask("  answer (? hint, s skip, q quit) > ");
            if (a == null || a.equalsIgnoreCase("q")) return false;
            if (a.equals("?")) { t.println(t.dim("  Hint: " + e.hint())); continue; }
            if (a.equalsIgnoreCase("s")) {
                t.println(t.dim("  Answer: " + e.answers().get(0) + "  (skipped, not counted)"));
                return true;
            }
            if (e.check(a)) {
                progress.markDone(e.id());
                t.println(t.good("  ✔ Correct!"));
                return true;
            }
            t.println(t.bad("  ✘ Not quite, try again."));
        }
    }

    // ---------- Progress ----------

    private void dashboard() {
        t.clear();
        t.println(t.heading("  Progress dashboard"));
        t.blank();
        for (Topic topic : curriculum.topics()) {
            int total = topic.exercises().size();
            int pct = total == 0 ? 0 : progress.doneIn(topic) * 100 / total;
            t.println(String.format("  %-32s %s", topic.title(), bar(pct)));
        }
        t.blank();
        int pct = progress.percent(curriculum);
        t.println("  Overall: " + bar(pct));
        if (pct >= ProgressTracker.GOAL_PERCENT) {
            t.println(t.good("  🎉 You have reached the " + ProgressTracker.GOAL_PERCENT + "% readiness goal!"));
        } else {
            t.println("  Solve " + progress.remainingToGoal(curriculum) + " more exercise(s) to reach "
                    + ProgressTracker.GOAL_PERCENT + "% readiness.");
        }
        t.blank();
        String in = t.ask("  Enter = back, type 'reset' to clear progress > ");
        if ("reset".equalsIgnoreCase(in)) progress.reset();
    }

    private String bar(int pct) {
        int filled = pct * 20 / 100;
        String color = pct >= ProgressTracker.GOAL_PERCENT ? t.good("█".repeat(filled)) : t.heading("█".repeat(filled));
        return color + t.dim("░".repeat(20 - filled)) + String.format(" %3d%%", pct);
    }

    // ---------- Settings ----------

    private void settings() {
        Theme theme = t.theme();
        while (true) {
            t.clear();
            t.println(t.heading("  Settings: colors"));
            t.blank();
            t.println("  1) Background  " + theme.background());
            t.println("  2) Text        " + theme.text());
            t.println("  3) Headings    " + theme.heading());
            t.println("  4) Show palette names");
            t.println("  5) Reset to defaults (black background)");
            t.println("  6) Back");
            t.blank();
            t.println("  Preview: " + t.heading("Heading") + " normal text");
            t.code("           int count = 42; // sample code");
            t.blank();
            String in = t.ask("  > ");
            if (in == null || in.equals("6")) return;
            switch (in) {
                case "1", "2", "3" -> {
                    String slot = Theme.SLOTS[Integer.parseInt(in) - 1];
                    String value = t.ask("  New " + slot + " color (name or #RRGGBB) > ");
                    if (value != null && !theme.set(slot, value)) {
                        t.println(t.bad("  Unknown color."));
                        t.pause();
                    }
                    theme.save();
                }
                case "4" -> {
                    t.blank();
                    Theme.PALETTE.forEach((name, hex) ->
                            t.println("   " + Theme.fg(hex) + "■ " + name + " " + hex + Theme.fg(theme.text())));
                    t.pause();
                }
                case "5" -> { theme.reset(); theme.save(); }
                default -> { }
            }
        }
    }
}
