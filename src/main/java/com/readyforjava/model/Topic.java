package com.readyforjava.model;

import java.util.List;

public record Topic(String id, String title, List<String> explanation,
                    List<Link> links, List<Exercise> exercises) {}
