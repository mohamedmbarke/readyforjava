package com.readyforjava.model;

import java.util.List;

/** A practice question. type is "short" (typed answer) or "choice" (letter answer). */
public record Exercise(String id, String type, String prompt, List<String> options,
                       List<String> answers, String hint) {

    public boolean check(String input) {
        String given = normalize(input);
        return answers.stream().anyMatch(a -> normalize(a).equals(given));
    }

    private static String normalize(String s) {
        return s.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
