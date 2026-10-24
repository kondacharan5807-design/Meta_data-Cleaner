package com.metashield.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Cross-platform Storage Properties
 * Guarantees zero hardcoded backslashes and strict NIO Path resolution across Windows and GCP Linux.
 */
@Component
public class StorageProperties {

    @Value("${metashield.storage.base-dir:temp/staging}")
    private String baseDirStr;

    @Value("${metashield.retention.ttl-seconds:3600}")
    private long ttlSeconds;

    private Path baseDir;
    private Path uploadsDir;
    private Path cleanedDir;

    @PostConstruct
    public void init() throws IOException {
        baseDir = Paths.get(baseDirStr).toAbsolutePath().normalize();
        uploadsDir = baseDir.resolve("uploads");
        cleanedDir = baseDir.resolve("cleaned");

        Files.createDirectories(uploadsDir);
        Files.createDirectories(cleanedDir);
    }

    public Path getBaseDir() {
        return baseDir;
    }

    public Path getUploadsDir() {
        return uploadsDir;
    }

    public Path getCleanedDir() {
        return cleanedDir;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }
}
