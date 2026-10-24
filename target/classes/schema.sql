-- MetaShield SQLite Database Schema
-- Strict Relational & Immutable Cryptographic Audit Ledger
-- Developers: Charan Konda (2511CS040113) & J. Naga Phani (2511CS040132)
-- Malla Reddy University - CS BETA - 17

CREATE TABLE IF NOT EXISTS files (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_uuid TEXT NOT NULL UNIQUE,
    original_filename TEXT NOT NULL,
    stored_path TEXT NOT NULL,
    cleaned_path TEXT,
    mime_type TEXT,
    file_size INTEGER NOT NULL,
    initial_sha256 TEXT NOT NULL,
    cleaned_sha256 TEXT,
    status TEXT NOT NULL DEFAULT 'STAGED', -- STAGED, SCANNED, SANITIZED, VERIFIED, EXPIRED_PURGED
    upload_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS metadata_tags (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_id INTEGER NOT NULL,
    category TEXT NOT NULL,
    tag_name TEXT NOT NULL,
    raw_value TEXT,
    severity TEXT NOT NULL, -- CRITICAL, HIGH, MEDIUM, LOW, SAFE
    risk_weight INTEGER DEFAULT 0,
    is_pii BOOLEAN DEFAULT 0,
    is_sanitized BOOLEAN DEFAULT 0,
    FOREIGN KEY(file_id) REFERENCES files(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS risk_assessments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_id INTEGER NOT NULL UNIQUE,
    overall_risk_score INTEGER NOT NULL,
    risk_level TEXT NOT NULL, -- LOW, MEDIUM, HIGH, SEVERE
    detected_hazards_json TEXT,
    compliance_json TEXT,
    summary TEXT,
    assessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(file_id) REFERENCES files(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS sanitization_policies (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    policy_name TEXT NOT NULL UNIQUE,
    preserve_copyright BOOLEAN NOT NULL DEFAULT 1,
    strip_gps BOOLEAN NOT NULL DEFAULT 1,
    strip_author BOOLEAN NOT NULL DEFAULT 1,
    strip_device_info BOOLEAN NOT NULL DEFAULT 1,
    strip_timestamps BOOLEAN NOT NULL DEFAULT 1,
    strip_revision_history BOOLEAN NOT NULL DEFAULT 1,
    is_default BOOLEAN NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS audit_ledger (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    block_index INTEGER NOT NULL UNIQUE,
    event_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    event_type TEXT NOT NULL, -- FILE_INGEST, SCAN_COMPLETE, SELECTIVE_WIPE, ZERO_TRACE_WIPE, RE_SCAN_VERIFIED, PURGED
    file_uuid TEXT NOT NULL,
    filename TEXT NOT NULL,
    operator_id TEXT NOT NULL,
    pre_hash TEXT,
    post_hash TEXT,
    details_json TEXT,
    prev_block_hash TEXT NOT NULL,
    current_block_hash TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS compliance_reports (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    report_uuid TEXT NOT NULL UNIQUE,
    file_uuid TEXT NOT NULL,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    institution TEXT NOT NULL,
    author_credit TEXT NOT NULL,
    verification_token TEXT NOT NULL,
    certificate_json TEXT NOT NULL
);

-- Seed Default Balanced Legal Mode Policy
INSERT OR IGNORE INTO sanitization_policies (id, policy_name, preserve_copyright, strip_gps, strip_author, strip_device_info, strip_timestamps, strip_revision_history, is_default)
VALUES (1, 'Balanced Legal Mode', 1, 1, 1, 1, 1, 1, 1);

INSERT OR IGNORE INTO sanitization_policies (id, policy_name, preserve_copyright, strip_gps, strip_author, strip_device_info, strip_timestamps, strip_revision_history, is_default)
VALUES (2, 'Maximum Zero-Trace Mode', 0, 1, 1, 1, 1, 1, 0);

CREATE INDEX IF NOT EXISTS idx_files_uuid ON files(file_uuid);
CREATE INDEX IF NOT EXISTS idx_files_expires ON files(expires_at);
CREATE INDEX IF NOT EXISTS idx_meta_file ON metadata_tags(file_id);
CREATE INDEX IF NOT EXISTS idx_audit_block ON audit_ledger(block_index);
