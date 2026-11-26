package com.readyforjava.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * Compiles and runs a learner's Java program in a temp directory with a time limit.
 * NOTE: this executes code on the host machine. Keep the server bound to localhost.
 */
public final class CodeRunner {
    public record Result(boolean compiled, String output) {}

    private static final Pattern PUBLIC_CLASS = Pattern.compile("public\\s+(?:final\\s+|abstract\\s+)*class\\s+(\\w+)");
    private static final int MAX_OUTPUT = 20_000;
    private static final int TIMEOUT_SECONDS = 5;

    private CodeRunner() {}

    public static Result run(String code) {
        Matcher m = PUBLIC_CLASS.matcher(code);
        String cls = m.find() ? m.group(1) : "Main";
        Path dir = null;
        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                return new Result(false, "No Java compiler found. Start readyforjava with a JDK, not a JRE.");
            }
            dir = Files.createTempDirectory("readyforjava");
            Path src = dir.resolve(cls + ".java");
            Files.writeString(src, code, StandardCharsets.UTF_8);

            ByteArrayOutputStream errors = new ByteArrayOutputStream();
            int rc = compiler.run(null, null, errors, "-d", dir.toString(), src.toString());
            if (rc != 0) return new Result(false, errors.toString(StandardCharsets.UTF_8).replace(dir.toString() + "/", ""));

            String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
            Process p = new ProcessBuilder(java, "-Xmx128m", "-cp", dir.toString(), cls)
                    .redirectErrorStream(true).start();
            p.getOutputStream().close();
            var reader = CompletableFuture.supplyAsync(() -> {
                try { return new String(p.getInputStream().readNBytes(MAX_OUTPUT), StandardCharsets.UTF_8); }
                catch (IOException e) { return ""; }
            });
            boolean finished = p.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) p.destroyForcibly();
            String out = reader.get(2, TimeUnit.SECONDS);
            if (!finished) out += "\n[Stopped: program ran longer than " + TIMEOUT_SECONDS + " seconds]";
            return new Result(true, out);
        } catch (Exception e) {
            return new Result(false, "Could not run your code: " + e.getMessage());
        } finally {
            if (dir != null) deleteQuietly(dir);
        }
    }

    private static void deleteQuietly(Path dir) {
        try (var walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) { }
    }
}