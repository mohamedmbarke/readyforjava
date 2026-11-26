package com.readyforjava.content;

import com.readyforjava.model.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Parses src/main/resources/curriculum.txt (format documented in the README). */
public final class CurriculumLoader {
    private static final String SEP = " @@ ";

    private CurriculumLoader() {}

    public static Curriculum load() throws IOException {
        try (var in = CurriculumLoader.class.getResourceAsStream("/curriculum.txt")) {
            if (in == null) throw new IOException("curriculum.txt not found on classpath");
            return parse(new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList());
        }
    }

    static Curriculum parse(List<String> lines) {
        List<Topic> topics = new ArrayList<>();
        String id = null, title = null;
        List<String> text = new ArrayList<>();
        List<Link> links = new ArrayList<>();
        List<Exercise> exercises = new ArrayList<>();
        List<Challenge> challenges = new ArrayList<>();
        boolean explaining = false;

        for (String line : lines) {
            if (line.startsWith("##")) continue;
            if (line.startsWith("@TOPIC ")) {
                if (id != null) topics.add(build(id, title, text, links, exercises, challenges));
                String[] f = line.substring(7).split(SEP);
                id = f[0].trim();
                title = f[1].trim();
                text = new ArrayList<>();
                links = new ArrayList<>();
                exercises = new ArrayList<>();
                challenges = new ArrayList<>();
                explaining = false;
            } else if (line.equals("@EXPLAIN")) {
                explaining = true;
            } else if (line.startsWith("@LINK ")) {
                explaining = false;
                String[] f = line.substring(6).split(SEP);
                links.add(new Link(f[0].trim(), f[1].trim(), f.length > 2 ? f[2].trim() : "site"));
            } else if (line.startsWith("@EX ")) {
                explaining = false;
                String[] f = line.substring(4).split(SEP, -1);
                exercises.add(new Exercise(id + "-" + (exercises.size() + 1), f[0].trim(),
                        nl(f[1]), split(f[2]), split(f[3]), f[4].trim()));
            } else if (line.startsWith("@CODE ")) {
                explaining = false;
                String[] f = line.substring(6).split(SEP, -1);
                challenges.add(new Challenge(id + "-c" + (challenges.size() + 1), f[0].trim(),
                        nl(f[1]), nl(f[2]), nl(f[3])));
            } else if (explaining) {
                text.add(line);
            }
        }
        if (id != null) topics.add(build(id, title, text, links, exercises, challenges));
        return new Curriculum(List.copyOf(topics));
    }

    private static Topic build(String id, String title, List<String> text, List<Link> links,
                               List<Exercise> exercises, List<Challenge> challenges) {
        while (!text.isEmpty() && text.get(text.size() - 1).isBlank()) text.remove(text.size() - 1);
        return new Topic(id, title, List.copyOf(text), List.copyOf(links),
                List.copyOf(exercises), List.copyOf(challenges));
    }

    private static String nl(String s) { return s.trim().replace("\\n", "\n"); }

    private static List<String> split(String field) {
        if (field.isBlank()) return List.of();
        return Arrays.stream(field.split(";;")).map(String::trim).toList();
    }
}