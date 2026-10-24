"""
MetaShield - Persistent Background Worker Daemon
Autonomously engineered for Charan Konda (2511CS040113) & J. Naga Phani (2511CS040132)
Malla Reddy University - CS BETA - 17

Communicates via standard I/O line-delimited JSON streams.
Eliminates process spawning overhead during high-volume batch processing.
"""

import sys
import json
import traceback
from pathlib import Path
from parser import parse_file
from risk_scorer import calculate_risk_profile
from sanitizer import sanitize_file

def handle_command(cmd: dict) -> dict:
    action = cmd.get("action")

    if action == "ping":
        return {"status": "ok", "message": "pong", "engine": "MetaShield Python Core v2.0"}

    elif action == "parse":
        file_path = cmd.get("file_path")
        if not file_path or not Path(file_path).exists():
            return {"status": "error", "message": f"File not found: {file_path}"}
        
        parsed = parse_file(file_path)
        risk = calculate_risk_profile(parsed.get("tags", []))
        return {
            "status": "ok",
            "format": parsed.get("format"),
            "tags": parsed.get("tags"),
            "risk_profile": risk
        }

    elif action == "sanitize":
        file_path = cmd.get("file_path")
        output_path = cmd.get("output_path")
        policy = cmd.get("policy", {})

        if not file_path or not Path(file_path).exists():
            return {"status": "error", "message": f"Source file not found: {file_path}"}

        san_res = sanitize_file(file_path, output_path, policy)
        return {
            "status": "ok",
            "sanitization": san_res
        }

    elif action == "verify":
        file_path = cmd.get("file_path")
        if not file_path or not Path(file_path).exists():
            return {"status": "error", "message": f"Target file not found: {file_path}"}

        # Re-parse cleaned file
        parsed = parse_file(file_path)
        risk = calculate_risk_profile(parsed.get("tags", []))
        
        # Check residual critical or high risk tags
        residual_hazards = [
            t for t in parsed.get("tags", [])
            if t.get("severity") in ["CRITICAL", "HIGH"]
        ]

        verified_clean = len(residual_hazards) == 0

        return {
            "status": "ok",
            "verified_clean": verified_clean,
            "residual_count": len(residual_hazards),
            "residual_tags": residual_hazards,
            "post_risk_score": risk.get("risk_score", 0),
            "post_risk_level": risk.get("risk_level", "LOW"),
            "compliance": risk.get("compliance", {}),
            "message": "Zero remnant privacy hazards confirmed." if verified_clean else "Remnant attributes detected."
        }

    else:
        return {"status": "error", "message": f"Unknown action: {action}"}

def run_daemon():
    # Signal readiness
    sys.stdout.write(json.dumps({"status": "ready", "engine": "MetaShield Daemon Initialized"}) + "\n")
    sys.stdout.flush()

    for line in sys.stdin:
        line = line.strip()
        if not line:
            continue
        if line == "QUIT":
            break

        try:
            cmd = json.loads(line)
            response = handle_command(cmd)
        except Exception as e:
            response = {
                "status": "error",
                "message": str(e),
                "trace": traceback.format_exc()
            }

        sys.stdout.write(json.dumps(response) + "\n")
        sys.stdout.flush()

if __name__ == "__main__":
    run_daemon()
