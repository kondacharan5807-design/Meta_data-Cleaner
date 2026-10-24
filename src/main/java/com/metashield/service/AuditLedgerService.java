package com.metashield.service;

import com.metashield.model.AuditLedgerEntry;
import com.metashield.repository.AuditLedgerRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class AuditLedgerService {

    private final AuditLedgerRepository auditLedgerRepository;

    public AuditLedgerService(AuditLedgerRepository auditLedgerRepository) {
        this.auditLedgerRepository = auditLedgerRepository;
    }

    public synchronized AuditLedgerEntry recordEvent(String eventType, String fileUuid, String filename,
                                                    String operatorId, String preHash, String postHash,
                                                    String detailsJson) {
        Optional<AuditLedgerEntry> lastOpt = auditLedgerRepository.findLatestBlock();
        String prevHash = lastOpt.map(AuditLedgerEntry::getCurrentBlockHash)
                .orElse("0000000000000000000000000000000000000000000000000000000000000000");

        String timestampStr = Instant.now().toString();
        String blockDataToHash = prevHash + ":" + timestampStr + ":" + eventType + ":" + fileUuid + ":" +
                (preHash != null ? preHash : "") + ":" +
                (postHash != null ? postHash : "") + ":" +
                (detailsJson != null ? detailsJson : "");

        String currentBlockHash = sha256(blockDataToHash);

        return auditLedgerRepository.appendBlock(eventType, fileUuid, filename, operatorId,
                preHash, postHash, detailsJson, currentBlockHash);
    }

    public List<AuditLedgerEntry> getRecentLogs() {
        return auditLedgerRepository.findAll();
    }

    public boolean verifyLedgerIntegrity() {
        List<AuditLedgerEntry> list = auditLedgerRepository.findAll();
        if (list.size() <= 1) return true;

        for (int i = 0; i < list.size() - 1; i++) {
            AuditLedgerEntry curr = list.get(i);
            AuditLedgerEntry prev = list.get(i + 1); // Ordered DESC
            if (!curr.getPrevBlockHash().equals(prev.getCurrentBlockHash())) {
                return false;
            }
        }
        return true;
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 error: " + e.getMessage(), e);
        }
    }
}
