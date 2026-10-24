package com.metashield.controller;

import com.metashield.service.ComplianceReportService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ComplianceReportController {

    private final ComplianceReportService complianceReportService;

    public ComplianceReportController(ComplianceReportService complianceReportService) {
        this.complianceReportService = complianceReportService;
    }

    @GetMapping("/{uuid}/certificate")
    public ResponseEntity<Map<String, Object>> getCertificate(@PathVariable String uuid) {
        try {
            return ResponseEntity.ok(complianceReportService.generateCertificate(uuid));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(value = "/{uuid}/print", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getPrintableCertificate(@PathVariable String uuid) {
        try {
            return ResponseEntity.ok(complianceReportService.generatePrintableHtml(uuid));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
