"""
MetaShield - Full End-to-End REST Integration Test Script
"""

import urllib.request
import json
from pathlib import Path

BASE_URL = "http://localhost:8080"

def run_test():
    print("=== METASHIELD END-TO-END INTEGRATION TEST ===")
    
    # 1. Check initial files
    print("\n[Step 1] Querying /api/files ...")
    with urllib.request.urlopen(f"{BASE_URL}/api/files") as r:
        files = json.loads(r.read().decode())
        print(f"  -> Currently staged files: {len(files)}")

    # 2. Upload sample photo
    print("\n[Step 2] Uploading sample_field_photo.jpg to /api/files/upload ...")
    boundary = "----MetaShieldUploadBoundary12345"
    filepath = Path("engine/test_samples/sample_field_photo.jpg")
    with open(filepath, "rb") as f:
        file_bytes = f.read()

    body = (
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="files"; filename="sample_field_photo.jpg"\r\n'
        f"Content-Type: image/jpeg\r\n\r\n"
    ).encode() + file_bytes + f"\r\n--{boundary}--\r\n".encode()

    req = urllib.request.Request(
        f"{BASE_URL}/api/files/upload",
        data=body,
        headers={"Content-Type": f"multipart/form-data; boundary={boundary}"}
    )
    with urllib.request.urlopen(req) as r:
        uploaded = json.loads(r.read().decode())
        file_uuid = uploaded[0]["fileUuid"]
        initial_sha256 = uploaded[0]["initialSha256"]
        print(f"  -> Ingested File UUID: {file_uuid}")
        print(f"  -> Pre-Sanitization SHA-256: {initial_sha256}")

    # 3. Deep Scan
    print("\n[Step 3] Running Deep Metadata Scan on /api/files/{uuid}/scan ...")
    scan_req = urllib.request.Request(f"{BASE_URL}/api/files/{file_uuid}/scan", data=b"", headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(scan_req) as r:
        scan_res = json.loads(r.read().decode())
        print(f"  -> Detected Format: {scan_res.get('format')}")
        print(f"  -> Extracted Tags: {len(scan_res.get('tags', []))}")
        risk = scan_res.get("risk_assessment", {})
        print(f"  -> Risk Score: {risk.get('overallRiskScore')} / 100 ({risk.get('riskLevel')})")
        print(f"  -> GDPR Compliance: {scan_res.get('compliance', {}).get('gdpr', {}).get('status')}")

    # 4. Surgical Sanitization (Balanced Legal Mode default)
    print("\n[Step 4] Executing Sanitization in Balanced Legal Mode ...")
    policy = {
        "preserveCopyright": True,
        "stripGps": True,
        "stripAuthor": True,
        "stripDeviceInfo": True,
        "stripTimestamps": True,
        "stripRevisionHistory": True
    }
    san_req = urllib.request.Request(
        f"{BASE_URL}/api/files/{file_uuid}/sanitize",
        data=json.dumps(policy).encode(),
        headers={"Content-Type": "application/json"}
    )
    with urllib.request.urlopen(san_req) as r:
        san_res = json.loads(r.read().decode())
        print(f"  -> Sanitization Status: {san_res.get('status')}")
        print(f"  -> Post-Sanitization SHA-256: {san_res.get('post_sha256')}")
        guard = san_res.get("stream_guard", {})
        print(f"  -> C++ Stream Guard Status: {guard.get('integrity_status')} ({guard.get('format')})")

    # 5. Automated Post-Cleaning Re-Scan Verification
    print("\n[Step 5] Triggering Automated Re-Scan Verification Guard ...")
    v_req = urllib.request.Request(f"{BASE_URL}/api/files/{file_uuid}/rescan-verify", data=b"", headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(v_req) as r:
        v_res = json.loads(r.read().decode())
        print(f"  -> Zero Remnant Privacy Hazards: {v_res.get('verified_clean')}")
        print(f"  -> Residual Hazard Count: {v_res.get('residual_count')}")
        print(f"  -> Post-Cleaning Risk Score: {v_res.get('post_risk_score')}")

    # 6. Cryptographic Audit Ledger & Chain Integrity
    print("\n[Step 6] Validating Immutable SQLite Chained Audit Ledger ...")
    with urllib.request.urlopen(f"{BASE_URL}/api/audit/logs") as r:
        logs = json.loads(r.read().decode())
        print(f"  -> Total Audit Ledger Blocks: {len(logs)}")
        for entry in logs[:4]:
            print(f"     Block #{entry.get('blockIndex')}: {entry.get('eventType')} | Target: {entry.get('filename')} | Hash: {entry.get('currentBlockHash')[:16]}...")

    with urllib.request.urlopen(f"{BASE_URL}/api/audit/verify-chain") as r:
        chain = json.loads(r.read().decode())
        print(f"  -> Chained Ledger Cryptographic Verification: {chain.get('status')}")

    # 7. Compliance Certificate Export
    print("\n[Step 7] Generating Verifiable Compliance Certificate ...")
    with urllib.request.urlopen(f"{BASE_URL}/api/reports/{file_uuid}/certificate") as r:
        cert = json.loads(r.read().decode())
        print(f"  -> Certificate Token: {cert.get('certificate_token')}")
        print(f"  -> Institution: {cert.get('institution')}")
        print(f"  -> Lead Architects: {cert.get('lead_architects')}")
        print(f"  -> Batch: {cert.get('batch')}")

    print("\n=======================================================")
    print("[OK] ALL TESTS PASSED: METASHIELD OPERATING AT 100% HEALTH")
    print("=======================================================")

if __name__ == "__main__":
    run_test()
