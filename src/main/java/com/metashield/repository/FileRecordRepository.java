package com.metashield.repository;

import com.metashield.config.DatabaseConfig.SqliteWriteQueue;
import com.metashield.model.FileRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class FileRecordRepository {

    private final JdbcTemplate jdbcTemplate;
    private final SqliteWriteQueue writeQueue;

    private final RowMapper<FileRecord> rowMapper = (rs, rowNum) -> {
        FileRecord r = new FileRecord();
        r.setId(rs.getLong("id"));
        r.setFileUuid(rs.getString("file_uuid"));
        r.setOriginalFilename(rs.getString("original_filename"));
        r.setStoredPath(rs.getString("stored_path"));
        r.setCleanedPath(rs.getString("cleaned_path"));
        r.setMimeType(rs.getString("mime_type"));
        r.setFileSize(rs.getLong("file_size"));
        r.setInitialSha256(rs.getString("initial_sha256"));
        r.setCleanedSha256(rs.getString("cleaned_sha256"));
        r.setStatus(rs.getString("status"));
        
        Timestamp upTs = rs.getTimestamp("upload_timestamp");
        if (upTs != null) r.setUploadTimestamp(upTs.toInstant());
        Timestamp exTs = rs.getTimestamp("expires_at");
        if (exTs != null) r.setExpiresAt(exTs.toInstant());
        return r;
    };

    public FileRecordRepository(JdbcTemplate jdbcTemplate, SqliteWriteQueue writeQueue) {
        this.jdbcTemplate = jdbcTemplate;
        this.writeQueue = writeQueue;
    }

    public FileRecord save(FileRecord f) {
        if (f.getId() == null) {
            String sql = "INSERT INTO files (file_uuid, original_filename, stored_path, cleaned_path, mime_type, file_size, initial_sha256, cleaned_sha256, status, upload_timestamp, expires_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            Timestamp upTs = f.getUploadTimestamp() != null ? Timestamp.from(f.getUploadTimestamp()) : Timestamp.from(Instant.now());
            Timestamp exTs = f.getExpiresAt() != null ? Timestamp.from(f.getExpiresAt()) : Timestamp.from(Instant.now().plusSeconds(3600));

            writeQueue.run(() -> {
                jdbcTemplate.update(sql, f.getFileUuid(), f.getOriginalFilename(), f.getStoredPath(), f.getCleanedPath(),
                        f.getMimeType(), f.getFileSize(), f.getInitialSha256(), f.getCleanedSha256(), f.getStatus(), upTs, exTs);
            }).join();

            Long id = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
            f.setId(id);
        } else {
            String sql = "UPDATE files SET cleaned_path = ?, cleaned_sha256 = ?, status = ? WHERE id = ?";
            writeQueue.run(() -> {
                jdbcTemplate.update(sql, f.getCleanedPath(), f.getCleanedSha256(), f.getStatus(), f.getId());
            }).join();
        }
        return f;
    }

    public Optional<FileRecord> findByUuid(String uuid) {
        List<FileRecord> list = jdbcTemplate.query("SELECT * FROM files WHERE file_uuid = ?", rowMapper, uuid);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<FileRecord> findById(Long id) {
        List<FileRecord> list = jdbcTemplate.query("SELECT * FROM files WHERE id = ?", rowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<FileRecord> findAll() {
        return jdbcTemplate.query("SELECT * FROM files ORDER BY id DESC LIMIT 100", rowMapper);
    }

    public List<FileRecord> findExpiredFiles(Instant now) {
        return jdbcTemplate.query("SELECT * FROM files WHERE expires_at <= ? AND status != 'EXPIRED_PURGED'",
                rowMapper, Timestamp.from(now));
    }

    public void updateStatus(Long id, String status) {
        writeQueue.run(() -> {
            jdbcTemplate.update("UPDATE files SET status = ? WHERE id = ?", status, id);
        }).join();
    }
}
