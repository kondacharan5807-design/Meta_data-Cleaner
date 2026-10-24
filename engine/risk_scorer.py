"""
MetaShield - Privacy Exposure Risk Scoring & Compliance Evaluation Engine
Autonomously engineered for Charan Konda (2511CS040113) & J. Naga Phani (2511CS040132)
Malla Reddy University - CS BETA - 17
"""

import sys
import json

def calculate_risk_profile(tags: list) -> dict:
    """
    Computes quantifiable privacy exposure risk score (0-100) and regulatory compliance flags.
    Weights:
      - GPS Geolocation: 35
      - Author & Organization PII: 25
      - Hardware & Device Serials: 20
      - Revision History & Tracked Changes: 15
      - Timestamps & System Signatures: 10
    """
    gps_detected = False
    pii_detected = False
    serial_detected = False
    revision_detected = False
    timestamp_detected = False
    copyright_preserved = False

    detected_hazards = []
    total_score = 0

    for tag in tags:
        tag_name = tag.get("tag_name", "").lower()
        category = tag.get("category", "").lower()
        val = tag.get("raw_value", "")
        weight = tag.get("weight", 0)

        # GPS Geolocation Check
        if "gps" in tag_name or "gps" in category or "latitude" in tag_name:
            gps_detected = True
            detected_hazards.append({
                "type": "GEOLOCATION_EXPOSURE",
                "label": "Precise GPS Coordinates Exposed",
                "tag": tag.get("tag_name"),
                "value": val,
                "severity": "CRITICAL"
            })
            total_score += weight

        # Author / PII
        elif tag.get("is_pii") and not any(k in tag_name for k in ["serial", "device", "mac"]):
            pii_detected = True
            detected_hazards.append({
                "type": "AUTHOR_IDENTITY_LEAK",
                "label": "User / Author Credentials Embedded",
                "tag": tag.get("tag_name"),
                "value": val,
                "severity": "HIGH"
            })
            total_score += weight

        # Serial / Hardware
        elif any(k in tag_name for k in ["serial", "device", "mac", "uniqueid"]):
            serial_detected = True
            detected_hazards.append({
                "type": "DEVICE_FINGERPRINT",
                "label": "Hardware Serial / Device Fingerprint Detected",
                "tag": tag.get("tag_name"),
                "value": val,
                "severity": "HIGH"
            })
            total_score += weight

        # Revisions / comments
        elif any(k in tag_name for k in ["revision", "comments", "tracked"]):
            revision_detected = True
            detected_hazards.append({
                "type": "REVISION_LEAK",
                "label": "Hidden Revisions / Unredacted Comments Discovered",
                "tag": tag.get("tag_name"),
                "value": val,
                "severity": "HIGH"
            })
            total_score += weight

        # Timestamps
        elif any(k in tag_name for k in ["date", "time", "created", "modified"]):
            timestamp_detected = True
            total_score += min(weight, 5)

        # Copyright check
        if any(k in tag_name for k in ["copyright", "rights", "license"]):
            copyright_preserved = True

    # Normalize score between 0 and 100
    risk_score = min(100, max(0, total_score))

    # Classification
    if risk_score >= 70:
        risk_level = "SEVERE"
        summary = "Critical privacy risk detected. Immediate selective sanitization required."
    elif risk_score >= 40:
        risk_level = "HIGH"
        summary = "Elevated privacy exposure detected with sensitive identifying tags."
    elif risk_score >= 15:
        risk_level = "MEDIUM"
        summary = "Moderate telemetry and timestamp attributes discovered."
    else:
        risk_level = "LOW"
        summary = "Minimal or nominal metadata footprint detected."

    # Regulatory compliance audits
    # GDPR: Article 5(1)(c) Data Minimisation, Article 25 Data Protection by Design
    gdpr_compliant = not (gps_detected or pii_detected or revision_detected)
    # CCPA: Sensitive Personal Information (SPI) detection (precise geolocation)
    ccpa_compliant = not gps_detected
    # HIPAA: Safe Harbor metadata requirements
    hipaa_compliant = not (pii_detected or gps_detected)
    # ISO 27001: Information asset security
    iso27001_status = "VERIFIABLE" if not (gps_detected or serial_detected) else "ACTION_REQUIRED"

    return {
        "risk_score": risk_score,
        "risk_level": risk_level,
        "summary": summary,
        "hazard_counts": {
            "geolocation": 1 if gps_detected else 0,
            "author_pii": 1 if pii_detected else 0,
            "device_serials": 1 if serial_detected else 0,
            "revision_history": 1 if revision_detected else 0
        },
        "detected_hazards": detected_hazards,
        "compliance": {
            "gdpr": {
                "compliant": gdpr_compliant,
                "articles": ["Art. 5(1)(c) Data Minimisation", "Art. 25 Privacy by Design"],
                "status": "PASS" if gdpr_compliant else "NON_COMPLIANT_PII_PRESENT"
            },
            "ccpa": {
                "compliant": ccpa_compliant,
                "section": "§ 1798.140 Sensitive Personal Information (SPI)",
                "status": "PASS" if ccpa_compliant else "NON_COMPLIANT_SPI_LOC"
            },
            "hipaa": {
                "compliant": hipaa_compliant,
                "rule": "45 CFR § 164.514(b) Safe Harbor Method",
                "status": "PASS" if hipaa_compliant else "ACTION_REQUIRED_PHI"
            },
            "iso27001": {
                "status": iso27001_status,
                "control": "A.8.11 Data Masking & Information Leakage Prevention"
            }
        },
        "legal_attributes": {
            "copyright_preserved": copyright_preserved
        }
    }

if __name__ == "__main__":
    test_tags = [
        {"tag_name": "GPSLatitude", "category": "GPS", "raw_value": "37.7749 N", "weight": 35, "is_pii": True},
        {"tag_name": "Artist", "category": "EXIF", "raw_value": "Dr. Vance", "weight": 25, "is_pii": True},
        {"tag_name": "Copyright", "category": "EXIF", "raw_value": "Copyright (C) 2026 MetaShield", "weight": 0, "is_pii": False}
    ]
    print(json.dumps(calculate_risk_profile(test_tags), indent=2))
