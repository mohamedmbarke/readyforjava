package com.readyforjava.ui;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Minimal Java syntax highlighter using standard editor-style colors. */
public final class Highlighter {
    private static final Set<String> KEYWORDS = Set.of(
            "public", "private", "protected", "static", "final", "void", "class", "interface",
            "extends", "implements", "new", "return", "if", "else", "for", "while", "do", "switch",
            "case", "break", "continue", "try", "catch", "finally", "throw", "throws", "this",
            "super", "import", "package", "int", "double", "boolean", "char", "long", "float",
            "byte", "short", "true", "false", "null", "abstract", "default");

    private static final Pattern TOKEN = Pattern.compile(
            "(//.*)|(\"(?:\\\\.|[^\"\\\\])*\")|(\\b\\d+(?:\\.\\d+)?[LlFfDd]?\\b)|(\\b[A-Za-z_]\\w*\\b)");

    private Highlighter() {}

    public static String line(String src, Theme theme) {
        Matcher m = TOKEN.matcher(src);
        StringBuilder out = new StringBuilder();
        int last = 0;
        while (m.find()) {
            out.append(src, last, m.start());
            String tok = m.group();
            String color = null;
            if (m.group(1) != null) color = Theme.PALETTE.get("gray");
            else if (m.group(2) != null) color = Theme.PALETTE.get("green");
            else if (m.group(3) != null) color = Theme.PALETTE.get("orange");
            else if (KEYWORDS.contains(tok)) color = Theme.PALETTE.get("magenta");
            else if (Character.isUpperCase(tok.charAt(0))) color = Theme.PALETTE.get("yellow");
            out.append(color == null ? tok : Theme.fg(color) + tok + Theme.fg(theme.text()));
            last = m.end();
        }
        return out.append(src.substring(last)).toString();
    }
}
