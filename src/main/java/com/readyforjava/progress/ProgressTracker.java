package com.readyforjava.progress;

import com.readyforjava.model.Curriculum;
import com.readyforjava.model.Topic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

/** Stores solved exercise ids in ~/.readyforjava/progress.properties. */
public final class ProgressTracker {
    public static final int GOAL_PERCENT = 70;
    private static final Path FILE =
            Path.of(System.getProperty("user.home"), ".readyforjava", "progress.properties");

    private final Set<String> done = new LinkedHashSet<>();

    public ProgressTracker() {
        try {
            if (Files.exists(FILE)) {
                Properties p = new Properties();
                try (var r = Files.newBufferedReader(FILE)) { p.load(r); }
                String raw = p.getProperty("done", "");
                if (!raw.isBlank()) done.addAll(Arrays.asList(raw.split(",")));
            }
        } catch (IOException ignored) { /* start fresh */ }
    }

    public boolean isDone(String exerciseId) { return done.contains(exerciseId); }

    public void markDone(String exerciseId) {
        if (done.add(exerciseId)) save();
    }

    public void reset() {
        done.clear();
        save();
    }

    public int doneIn(Topic topic) {
        return (int) topic.exercises().stream().filter(e -> done.contains(e.id())).count();
    }

    public int doneTotal(Curriculum c) {
        return c.topics().stream().mapToInt(this::doneIn).sum();
    }

    public int percent(Curriculum c) {
        int total = c.totalExercises();
        return total == 0 ? 0 : doneTotal(c) * 100 / total;
    }

    /** Exercises still needed to reach the readiness goal. */
    public int remainingToGoal(Curriculum c) {
        int target = (int) Math.ceil(c.totalExercises() * GOAL_PERCENT / 100.0);
        return Math.max(0, target - doneTotal(c));
    }

    private void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Properties p = new Properties();
            p.setProperty("done", String.join(",", done));
            try (var w = Files.newBufferedWriter(FILE)) { p.store(w, "readyforjava progress"); }
        } catch (IOException ignored) { /* non-fatal */ }
    }
}
