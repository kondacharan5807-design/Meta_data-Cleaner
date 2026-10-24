package com.metashield.repository;

import com.metashield.config.DatabaseConfig.SqliteWriteQueue;
import com.metashield.model.MetadataTag;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MetadataTagRepository {

    private final JdbcTemplate jdbcTemplate;
    private final SqliteWriteQueue writeQueue;

    private final RowMapper<MetadataTag> rowMapper = (rs, rowNum) -> {
        MetadataTag t = new MetadataTag();
        t.setId(rs.getLong("id"));
        t.setFileId(rs.getLong("file_id"));
        t.setCategory(rs.getString("category"));
        t.setTagName(rs.getString("tag_name"));
        t.setRawValue(rs.getString("raw_value"));
        t.setSeverity(rs.getString("severity"));
        t.setRiskWeight(rs.getInt("risk_weight"));
        t.setIsPii(rs.getBoolean("is_pii"));
        t.setIsSanitized(rs.getBoolean("is_sanitized"));
        return t;
    };

    public MetadataTagRepository(JdbcTemplate jdbcTemplate, SqliteWriteQueue writeQueue) {
        this.jdbcTemplate = jdbcTemplate;
        this.writeQueue = writeQueue;
    }

    public void saveAll(List<MetadataTag> tags) {
        if (tags == null || tags.isEmpty()) return;
        String sql = "INSERT INTO metadata_tags (file_id, category, tag_name, raw_value, severity, risk_weight, is_pii, is_sanitized) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        writeQueue.run(() -> {
            for (MetadataTag t : tags) {
                jdbcTemplate.update(sql, t.getFileId(), t.getCategory(), t.getTagName(), t.getRawValue(),
                        t.getSeverity(), t.getRiskWeight(), t.getIsPii(), t.getIsSanitized());
            }
        }).join();
    }

    public List<MetadataTag> findByFileId(Long fileId) {
        return jdbcTemplate.query("SELECT * FROM metadata_tags WHERE file_id = ? ORDER BY risk_weight DESC, id ASC",
                rowMapper, fileId);
    }

    public void markAllSanitized(Long fileId) {
        writeQueue.run(() -> {
            jdbcTemplate.update("UPDATE metadata_tags SET is_sanitized = 1 WHERE file_id = ?", fileId);
        }).join();
    }

    public void deleteByFileId(Long fileId) {
        writeQueue.run(() -> {
            jdbcTemplate.update("DELETE FROM metadata_tags WHERE file_id = ?", fileId);
        }).join();
    }
}
