package com.readyforjava;

import java.util.List;

import com.readyforjava.model.Curriculum;
import com.readyforjava.model.Exercise;
import com.readyforjava.model.Link;
import com.readyforjava.model.Topic;
import com.readyforjava.progress.ProgressTracker;
import com.readyforjava.ui.Terminal;
import com.readyforjava.ui.Theme;

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
                if (n >= 1 && n <= topics.size()) openTopic(n - 1);
            } catch (NumberFormatException ignored) { }
        }
    }

    private void openTopic(int index) {
        List<Topic> topics = curriculum.topics();
        while (true) {
            Topic topic = topics.get(index);
            t.clear();
            t.println(t.heading("  " + topic.title() + "  (" + (index + 1) + "/" + topics.size() + ")"));
            t.blank();
            for (String line : topic.explanation()) {
                if (line.startsWith("    ")) t.code("  " + line);
                else t.println("  " + line);
            }
            t.blank();
            t.println(t.heading("  Keep learning (videos, websites, docs)"));
            for (Link l : topic.links()) t.println("   • [" + l.type() + "] " + l.title() + "  " + t.link(l.url()));
            t.blank();
            String in = t.ask("  n = next topic, p = previous, x = practice, b = back to list > ");
            if (in == null || in.equalsIgnoreCase("b")) return;
            switch (in.toLowerCase()) {
                case "n" -> index = Math.min(index + 1, topics.size() - 1);
                case "p" -> index = Math.max(index - 1, 0);
                case "x" -> practice(topic.title(), topic.exercises());
                default -> { }
            }
        }
    }

    // ---------- Practice ----------

    private void practiceAll() {
        List<Exercise> todo = curriculum.topics().stream()
                .flatMap(tp -> tp.exercises().stream())
                .filter(e -> !progress.isDone(e.id())).toList();
        practice("All unsolved exercises", todo);
    }

    private static final int NEXT = 0, BACK = 1, QUIT = 2;

    private void practice(String title, List<Exercise> list) {
        t.clear();
        t.println(t.heading("  Practice: " + title));
        if (list.isEmpty()) {
            t.println(t.good("  Nothing to practice here."));
            t.pause();
            return;
        }
        int i = 0;
        while (i < list.size()) {
            int r = ask(list.get(i), i + 1, list.size());
            if (r == QUIT) return;
            i = r == BACK ? Math.max(0, i - 1) : i + 1;
        }
        t.blank();
        t.println(t.good("  Set complete! Readiness is now " + progress.percent(curriculum) + "%."));
        t.pause();
    }

    private int ask(Exercise e, int n, int total) {
        t.blank();
        t.println(t.heading("  Exercise " + n + "/" + total) + (progress.isDone(e.id()) ? "  " + t.good("(solved)") : ""));
        String[] lines = e.prompt().split("\n");
        t.println("  " + lines[0]);
        for (int i = 1; i < lines.length; i++) t.code("      " + lines[i]);
        for (String option : e.options()) t.println("      " + option);
        while (true) {
            String a = t.ask("  answer (? hint, < back, > next, s show answer, q quit) > ");
            if (a == null || a.equalsIgnoreCase("q")) return QUIT;
            if (a.equals("<")) return BACK;
            if (a.equals(">")) return NEXT;
            if (a.equals("?")) { t.println(t.dim("  Hint: " + e.hint())); continue; }
            if (a.equalsIgnoreCase("s")) {
                t.println(t.dim("  Answer: " + e.answers().get(0) + "  (not counted)"));
                return NEXT;
            }
            if (e.check(a)) {
                progress.markDone(e.id());
                t.println(t.good("  ✔ Correct!"));
                return NEXT;
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