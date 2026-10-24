package com.metashield.repository;

import com.metashield.config.DatabaseConfig.SqliteWriteQueue;
import com.metashield.model.RiskAssessment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class RiskAssessmentRepository {

    private final JdbcTemplate jdbcTemplate;
    private final SqliteWriteQueue writeQueue;

    private final RowMapper<RiskAssessment> rowMapper = (rs, rowNum) -> {
        RiskAssessment r = new RiskAssessment();
        r.setId(rs.getLong("id"));
        r.setFileId(rs.getLong("file_id"));
        r.setOverallRiskScore(rs.getInt("overall_risk_score"));
        r.setRiskLevel(rs.getString("risk_level"));
        r.setDetectedHazardsJson(rs.getString("detected_hazards_json"));
        r.setComplianceJson(rs.getString("compliance_json"));
        r.setSummary(rs.getString("summary"));
        Timestamp ts = rs.getTimestamp("assessed_at");
        if (ts != null) r.setAssessedAt(ts.toInstant());
        return r;
    };

    public RiskAssessmentRepository(JdbcTemplate jdbcTemplate, SqliteWriteQueue writeQueue) {
        this.jdbcTemplate = jdbcTemplate;
        this.writeQueue = writeQueue;
    }

    public RiskAssessment save(RiskAssessment r) {
        String sql = "INSERT OR REPLACE INTO risk_assessments (file_id, overall_risk_score, risk_level, detected_hazards_json, compliance_json, summary, assessed_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        Timestamp ts = r.getAssessedAt() != null ? Timestamp.from(r.getAssessedAt()) : Timestamp.from(Instant.now());

        writeQueue.run(() -> {
            jdbcTemplate.update(sql, r.getFileId(), r.getOverallRiskScore(), r.getRiskLevel(),
                    r.getDetectedHazardsJson(), r.getComplianceJson(), r.getSummary(), ts);
        }).join();

        Long id = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
        r.setId(id);
        return r;
    }

    public Optional<RiskAssessment> findByFileId(Long fileId) {
        List<RiskAssessment> list = jdbcTemplate.query("SELECT * FROM risk_assessments WHERE file_id = ?", rowMapper, fileId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
