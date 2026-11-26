package com.readyforjava.web;

import com.readyforjava.model.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/** Serves the single-page site and a tiny JSON API using only the JDK. */
public final class WebServer {
    private static final String JSON = "application/json; charset=utf-8";

    private final Map<String, Exercise> exercises = new HashMap<>();
    private final Map<String, Challenge> challenges = new HashMap<>();
    private final String curriculumJson;

    public WebServer(Curriculum curriculum) {
        for (Topic t : curriculum.topics()) {
            t.exercises().forEach(e -> exercises.put(e.id(), e));
            t.challenges().forEach(c -> challenges.put(c.id(), c));
        }
        this.curriculumJson = toJson(curriculum);
    }

    /** Binds to localhost only, because /api/run executes learner code. */
    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        server.createContext("/", this::page);
        server.createContext("/api/curriculum", ex -> send(ex, 200, JSON, curriculumJson));
        server.createContext("/api/check", this::check);
        server.createContext("/api/run", this::run);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
    }

    private void page(HttpExchange ex) throws IOException {
        if (!ex.getRequestURI().getPath().equals("/")) { send(ex, 404, "text/plain", "Not found"); return; }
        try (var in = WebServer.class.getResourceAsStream("/web/index.html")) {
            send(ex, 200, "text/html; charset=utf-8", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private void check(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("POST")) { send(ex, 405, JSON, "{\"error\":\"POST only\"}"); return; }
        Map<String, String> f = form(body(ex));
        Exercise e = exercises.get(f.get("id"));
        if (e == null) { send(ex, 404, JSON, "{\"error\":\"unknown exercise\"}"); return; }
        boolean ok = e.check(f.getOrDefault("answer", ""));
        boolean reveal = "1".equals(f.get("reveal"));
        send(ex, 200, JSON, "{\"correct\":" + ok + (ok || reveal ? ",\"answer\":" + q(e.answers().get(0)) : "") + "}");
    }

    private void run(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("POST")) { send(ex, 405, JSON, "{\"error\":\"POST only\"}"); return; }
        Challenge ch = challenges.get(form(ex.getRequestURI().getRawQuery()).get("challenge"));
        CodeRunner.Result r = CodeRunner.run(body(ex));
        String passed = ch == null ? "" : ",\"passed\":" + (r.compiled() && norm(r.output()).equals(norm(ch.expected())));
        send(ex, 200, JSON, "{\"compiled\":" + r.compiled() + ",\"output\":" + q(r.output()) + passed + "}");
    }

    // ---------- helpers ----------

    private static String norm(String s) {
        StringBuilder sb = new StringBuilder();
        for (String line : s.replace("\r\n", "\n").split("\n", -1)) sb.append(line.stripTrailing()).append('\n');
        return sb.toString().strip();
    }

    private static String body(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readNBytes(200_000), StandardCharsets.UTF_8);
    }

    private static Map<String, String> form(String s) {
        Map<String, String> map = new HashMap<>();
        if (s == null) return map;
        for (String pair : s.split("&")) {
            int i = pair.indexOf('=');
            if (i < 0) continue;
            map.put(URLDecoder.decode(pair.substring(0, i), StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8));
        }
        return map;
    }

    private static void send(HttpExchange ex, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(status, bytes.length);
        try (var out = ex.getResponseBody()) { out.write(bytes); }
    }

    private static String q(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> { if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c); }
            }
        }
        return sb.append('"').toString();
    }

    private static String arr(List<String> items) {
        return "[" + String.join(",", items.stream().map(WebServer::q).toList()) + "]";
    }

    /** Answers and expected outputs are deliberately left out of the payload. */
    private static String toJson(Curriculum c) {
        StringBuilder sb = new StringBuilder("{\"topics\":[");
        for (int i = 0; i < c.topics().size(); i++) {
            Topic t = c.topics().get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"id\":").append(q(t.id())).append(",\"title\":").append(q(t.title()))
              .append(",\"text\":").append(arr(t.explanation())).append(",\"links\":[");
            sb.append(String.join(",", t.links().stream().map(l ->
                    "{\"title\":" + q(l.title()) + ",\"url\":" + q(l.url()) + ",\"type\":" + q(l.type()) + "}").toList()));
            sb.append("],\"exercises\":[");
            sb.append(String.join(",", t.exercises().stream().map(e ->
                    "{\"id\":" + q(e.id()) + ",\"type\":" + q(e.type()) + ",\"prompt\":" + q(e.prompt())
                            + ",\"options\":" + arr(e.options()) + ",\"hint\":" + q(e.hint()) + "}").toList()));
            sb.append("],\"challenges\":[");
            sb.append(String.join(",", t.challenges().stream().map(h ->
                    "{\"id\":" + q(h.id()) + ",\"title\":" + q(h.title()) + ",\"prompt\":" + q(h.prompt())
                            + ",\"starter\":" + q(h.starter()) + "}").toList()));
            sb.append("]}");
        }
        return sb.append("]}").toString();
    }
}