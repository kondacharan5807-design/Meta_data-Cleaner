package com.metashield.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metashield.model.FileRecord;
import com.metashield.model.MetadataTag;
import com.metashield.model.RiskAssessment;
import com.metashield.repository.FileRecordRepository;
import com.metashield.repository.MetadataTagRepository;
import com.metashield.repository.RiskAssessmentRepository;
import com.metashield.service.AuditLedgerService;
import com.metashield.service.EngineOrchestrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/files")
public class MetadataScanController {

    private final FileRecordRepository fileRecordRepository;
    private final MetadataTagRepository metadataTagRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final EngineOrchestrationService orchestrationService;
    private final AuditLedgerService auditLedgerService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MetadataScanController(FileRecordRepository fileRecordRepository,
                                  MetadataTagRepository metadataTagRepository,
                                  RiskAssessmentRepository riskAssessmentRepository,
                                  EngineOrchestrationService orchestrationService,
                                  AuditLedgerService auditLedgerService) {
        this.fileRecordRepository = fileRecordRepository;
        this.metadataTagRepository = metadataTagRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.orchestrationService = orchestrationService;
        this.auditLedgerService = auditLedgerService;
    }

    @PostMapping("/{uuid}/scan")
    public ResponseEntity<Map<String, Object>> scanFile(@PathVariable String uuid) {
        Optional<FileRecord> fileOpt = fileRecordRepository.findByUuid(uuid);
        if (fileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        FileRecord file = fileOpt.get();
        Path filePath = Paths.get(file.getStoredPath());

        try {
            JsonNode engineResponse = orchestrationService.parseFile(filePath);
            if (!"ok".equals(engineResponse.path("status").asText())) {
                return ResponseEntity.internalServerError().body(Map.of("error", engineResponse.path("message").asText()));
            }

            JsonNode tagsNode = engineResponse.path("tags");
            JsonNode riskNode = engineResponse.path("risk_profile");

            // Persist tags to SQLite
            List<MetadataTag> tagsToSave = new ArrayList<>();
            if (tagsNode.isArray()) {
                metadataTagRepository.deleteByFileId(file.getId());
                for (JsonNode t : tagsNode) {
                    MetadataTag tag = new MetadataTag();
                    tag.setFileId(file.getId());
                    tag.setCategory(t.path("category").asText("EXIF"));
                    tag.setTagName(t.path("tag_name").asText());
                    tag.setRawValue(t.path("raw_value").asText());
                    tag.setSeverity(t.path("severity").asText("LOW"));
                    tag.setRiskWeight(t.path("weight").asInt(0));
                    tag.setIsPii(t.path("is_pii").asBoolean(false));
                    tag.setIsSanitized(false);
                    tagsToSave.add(tag);
                }
                metadataTagRepository.saveAll(tagsToSave);
            }

            // Persist Risk Assessment to SQLite
            RiskAssessment risk = new RiskAssessment();
            risk.setFileId(file.getId());
            risk.setOverallRiskScore(riskNode.path("risk_score").asInt(0));
            risk.setRiskLevel(riskNode.path("risk_level").asText("LOW"));
            risk.setDetectedHazardsJson(objectMapper.writeValueAsString(riskNode.path("detected_hazards")));
            risk.setComplianceJson(objectMapper.writeValueAsString(riskNode.path("compliance")));
            risk.setSummary(riskNode.path("summary").asText());
            risk.setAssessedAt(Instant.now());
            riskAssessmentRepository.save(risk);

            // Update file status
            fileRecordRepository.updateStatus(file.getId(), "SCANNED");

            // Audit Log
            auditLedgerService.recordEvent(
                    "SCAN_COMPLETE",
                    file.getFileUuid(),
                    file.getOriginalFilename(),
                    "INTELLIGENCE_LAYER",
                    file.getInitialSha256(),
                    null,
                    "{\"risk_score\":" + risk.getOverallRiskScore() + ",\"risk_level\":\"" + risk.getRiskLevel() + "\"}"
            );

            Map<String, Object> result = new HashMap<>();
            result.put("file", file);
            result.put("format", engineResponse.path("format").asText());
            result.put("tags", tagsToSave);
            result.put("risk_assessment", risk);
            result.put("compliance", objectMapper.treeToValue(riskNode.path("compliance"), Map.class));
            result.put("hazard_counts", objectMapper.treeToValue(riskNode.path("hazard_counts"), Map.class));
            result.put("legal_attributes", objectMapper.treeToValue(riskNode.path("legal_attributes"), Map.class));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{uuid}/metadata")
    public ResponseEntity<List<MetadataTag>> getMetadata(@PathVariable String uuid) {
        return fileRecordRepository.findByUuid(uuid)
                .map(f -> ResponseEntity.ok(metadataTagRepository.findByFileId(f.getId())))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{uuid}/risk")
    public ResponseEntity<RiskAssessment> getRiskAssessment(@PathVariable String uuid) {
        return fileRecordRepository.findByUuid(uuid)
                .flatMap(f -> riskAssessmentRepository.findByFileId(f.getId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
