package com.restautomation.commons.utilities.common;

import com.restautomation.exceptions.FixtureNotFoundException;
import com.restautomation.exceptions.FrameworkException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resource access that works the same for classpath fixtures and files on disk.
 * Paths are tried on the classpath first, then on the file system.
 */
public final class FileUtils {

    private FileUtils() {
    }

    public static boolean exists(String path) {
        return classpathUrl(path) != null || Files.exists(Paths.get(path));
    }

    public static InputStream openStream(String path) {
        try {
            URL url = classpathUrl(path);
            if (url != null) {
                return url.openStream();
            }
            Path file = Paths.get(path);
            if (Files.exists(file)) {
                return Files.newInputStream(file);
            }
        } catch (IOException e) {
            throw new FrameworkException("Unable to open " + path, e);
        }
        throw new FixtureNotFoundException("File not found on classpath or disk: " + path);
    }

    public static String readAsString(String path) {
        try (InputStream in = openStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read " + path, e);
        }
    }

    public static byte[] readAsBytes(String path) {
        try (InputStream in = openStream(path)) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read " + path, e);
        }
    }

    public static Path write(String path, String content) {
        try {
            Path file = Paths.get(path);
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            return Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to write " + path, e);
        }
    }

    public static String join(String... segments) {
        StringBuilder builder = new StringBuilder();
        for (String segment : segments) {
            if (segment == null || segment.isBlank()) {
                continue;
            }
            String clean = segment.replace('\\', '/');
            if (builder.length() > 0) {
                builder.append('/');
                clean = clean.replaceAll("^/+", "");
            }
            builder.append(clean.replaceAll("/+$", ""));
        }
        return builder.toString();
    }

    public static String extension(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? "" : path.substring(dot + 1).toLowerCase();
    }

    /** Turns any text (e.g. a scenario name) into a safe file name. */
    public static String safeFileName(String text) {
        return text.replaceAll("[^A-Za-z0-9._-]+", "_");
    }

    private static URL classpathUrl(String path) {
        String clean = path.replace('\\', '/').replaceFirst("^classpath:", "").replaceFirst("^/", "");
        return Thread.currentThread().getContextClassLoader().getResource(clean);
    }
}
