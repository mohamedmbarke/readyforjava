package com.readyforjava.model;

/** type is one of: video, site, docs, practice. */
public record Link(String title, String url, String type) {}