package com.readyforjava.model;

import java.util.List;

public record Curriculum(List<Topic> topics) {
    public int totalExercises() {
        return topics.stream().mapToInt(t -> t.exercises().size()).sum();
    }
}