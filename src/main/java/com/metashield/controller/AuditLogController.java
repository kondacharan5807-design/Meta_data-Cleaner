package com.metashield.controller;

import com.metashield.model.AuditLedgerEntry;
import com.metashield.service.AuditLedgerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
public class AuditLogController {

    private final AuditLedgerService auditLedgerService;

    public AuditLogController(AuditLedgerService auditLedgerService) {
        this.auditLedgerService = auditLedgerService;
    }

    @GetMapping("/logs")
    public ResponseEntity<List<AuditLedgerEntry>> getLogs() {
        return ResponseEntity.ok(auditLedgerService.getRecentLogs());
    }

    @GetMapping("/verify-chain")
    public ResponseEntity<Map<String, Object>> verifyChain() {
        boolean valid = auditLedgerService.verifyLedgerIntegrity();
        return ResponseEntity.ok(Map.of(
                "integrity_valid", valid,
                "cryptographic_algorithm", "SHA-256 Chained Hash Ledger",
                "status", valid ? "UNBROKEN_TAMPER_EVIDENT_CHAIN" : "CHAIN_INTEGRITY_COMPROMISED"
        ));
    }
}
