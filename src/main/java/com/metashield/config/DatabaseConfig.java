package com.metashield.config;

import org.sqlite.SQLiteDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.FileCopyUtils;

import javax.sql.DataSource;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.*;

/**
 * SQLite Configuration with Concurrency Control & Write Serialization
 * Eliminates SQLITE_BUSY contention via a dedicated single-threaded write queue.
 */
@Configuration
public class DatabaseConfig {

    @Value("${spring.datasource.url:jdbc:sqlite:data/metashield.db}")
    private String dbUrl;

    // Dedicated single-threaded executor for all SQLite write operations
    private final ExecutorService sqliteWriteExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "sqlite-write-worker");
        t.setDaemon(true);
        return t;
    });

    @Bean
    public DataSource dataSource() {
        try {
            // Ensure data directory exists cross-platform
            Path dbPath = Paths.get("data");
            if (!Files.exists(dbPath)) {
                Files.createDirectories(dbPath);
            }
        } catch (Exception ignored) {}

        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl(dbUrl);

        // Apply WAL mode and pragmas
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode = WAL;");
            stmt.execute("PRAGMA synchronous = NORMAL;");
            stmt.execute("PRAGMA busy_timeout = 15000;");
            stmt.execute("PRAGMA foreign_keys = ON;");
        } catch (Exception e) {
            System.err.println("Notice initializing SQLite PRAGMAs: " + e.getMessage());
        }

        initializeSchema(ds);
        return ds;
    }

    private void initializeSchema(DataSource ds) {
        try {
            ClassPathResource resource = new ClassPathResource("schema.sql");
            String sql = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
            
            try (Connection conn = ds.getConnection();
                 Statement stmt = conn.createStatement()) {
                for (String command : sql.split(";")) {
                    String trimmed = command.trim();
                    if (!trimmed.isEmpty()) {
                        stmt.execute(trimmed);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Notice applying schema.sql: " + e.getMessage());
        }
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public SqliteWriteQueue sqliteWriteQueue(JdbcTemplate jdbcTemplate) {
        return new SqliteWriteQueue(sqliteWriteExecutor, jdbcTemplate);
    }

    /**
     * Component for safely executing serialized writes to SQLite
     */
    public static class SqliteWriteQueue {
        private final ExecutorService executor;
        private final JdbcTemplate jdbcTemplate;

        public SqliteWriteQueue(ExecutorService executor, JdbcTemplate jdbcTemplate) {
            this.executor = executor;
            this.jdbcTemplate = jdbcTemplate;
        }

        public <T> CompletableFuture<T> submit(Callable<T> task) {
            CompletableFuture<T> future = new CompletableFuture<>();
            executor.submit(() -> {
                try {
                    future.complete(task.call());
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
            return future;
        }

        public CompletableFuture<Void> run(Runnable task) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            executor.submit(() -> {
                try {
                    task.run();
                    future.complete(null);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
            return future;
        }

        public JdbcTemplate getJdbcTemplate() {
            return jdbcTemplate;
        }
    }
}
