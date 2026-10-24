package com.metashield.repository;

import com.metashield.config.DatabaseConfig.SqliteWriteQueue;
import com.metashield.model.AuditLedgerEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class AuditLedgerRepository {

    private final JdbcTemplate jdbcTemplate;
    private final SqliteWriteQueue writeQueue;

    private final RowMapper<AuditLedgerEntry> rowMapper = (rs, rowNum) -> {
        AuditLedgerEntry e = new AuditLedgerEntry();
        e.setId(rs.getLong("id"));
        e.setBlockIndex(rs.getLong("block_index"));
        Timestamp ts = rs.getTimestamp("event_timestamp");
        if (ts != null) e.setEventTimestamp(ts.toInstant());
        e.setEventType(rs.getString("event_type"));
        e.setFileUuid(rs.getString("file_uuid"));
        e.setFilename(rs.getString("filename"));
        e.setOperatorId(rs.getString("operator_id"));
        e.setPreHash(rs.getString("pre_hash"));
        e.setPostHash(rs.getString("post_hash"));
        e.setDetailsJson(rs.getString("details_json"));
        e.setPrevBlockHash(rs.getString("prev_block_hash"));
        e.setCurrentBlockHash(rs.getString("current_block_hash"));
        return e;
    };

    public AuditLedgerRepository(JdbcTemplate jdbcTemplate, SqliteWriteQueue writeQueue) {
        this.jdbcTemplate = jdbcTemplate;
        this.writeQueue = writeQueue;
    }

    public synchronized AuditLedgerEntry appendBlock(String eventType, String fileUuid, String filename,
                                                    String operatorId, String preHash, String postHash,
                                                    String detailsJson, String currentBlockHash) {
        return writeQueue.submit(() -> {
            Optional<AuditLedgerEntry> lastOpt = findLatestBlockInternal();
            long newIndex = lastOpt.map(b -> b.getBlockIndex() + 1).orElse(1L);
            String prevHash = lastOpt.map(AuditLedgerEntry::getCurrentBlockHash)
                    .orElse("0000000000000000000000000000000000000000000000000000000000000000");

            String sql = "INSERT INTO audit_ledger (block_index, event_timestamp, event_type, file_uuid, filename, operator_id, pre_hash, post_hash, details_json, prev_block_hash, current_block_hash) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            Timestamp now = Timestamp.from(Instant.now());

            jdbcTemplate.update(sql, newIndex, now, eventType, fileUuid, filename, operatorId, preHash, postHash, detailsJson, prevHash, currentBlockHash);

            AuditLedgerEntry entry = new AuditLedgerEntry();
            entry.setBlockIndex(newIndex);
            entry.setEventTimestamp(now.toInstant());
            entry.setEventType(eventType);
            entry.setFileUuid(fileUuid);
            entry.setFilename(filename);
            entry.setOperatorId(operatorId);
            entry.setPreHash(preHash);
            entry.setPostHash(postHash);
            entry.setDetailsJson(detailsJson);
            entry.setPrevBlockHash(prevHash);
            entry.setCurrentBlockHash(currentBlockHash);
            return entry;
        }).join();
    }

    private Optional<AuditLedgerEntry> findLatestBlockInternal() {
        List<AuditLedgerEntry> list = jdbcTemplate.query(
                "SELECT * FROM audit_ledger ORDER BY block_index DESC LIMIT 1", rowMapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<AuditLedgerEntry> findLatestBlock() {
        return findLatestBlockInternal();
    }

    public List<AuditLedgerEntry> findAll() {
        return jdbcTemplate.query("SELECT * FROM audit_ledger ORDER BY block_index DESC LIMIT 100", rowMapper);
    }
}
