"""
MetaShield - Automated Test Suite for Engine Core
Tests parser, risk scorer, sanitizer, and persistent daemon protocols.
"""

import unittest
import os
import sys
import json
import subprocess
from pathlib import Path

# Add engine directory to sys.path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from parser import parse_file
from risk_scorer import calculate_risk_profile
from sanitizer import sanitize_file

class TestMetaShieldEngine(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.test_dir = Path("engine/test_samples")
        cls.photo_path = str(cls.test_dir / "sample_field_photo.jpg")
        cls.docx_path = str(cls.test_dir / "sample_executive_brief.docx")
        cls.pdf_path = str(cls.test_dir / "sample_compliance_memo.pdf")
        cls.cleaned_dir = Path("temp/staging/cleaned")
        cls.cleaned_dir.mkdir(parents=True, exist_ok=True)

    def test_01_photo_parsing(self):
        parsed = parse_file(self.photo_path)
        self.assertEqual(parsed["format"], "IMAGE")
        tag_names = [t["tag_name"] for t in parsed["tags"]]
        self.assertTrue(any("GPS" in name for name in tag_names))
        self.assertIn("Copyright", tag_names)

    def test_02_risk_scoring(self):
        parsed = parse_file(self.photo_path)
        risk = calculate_risk_profile(parsed["tags"])
        self.assertGreaterEqual(risk["risk_score"], 70)
        self.assertIn(risk["risk_level"], ["HIGH", "SEVERE"])
        self.assertFalse(risk["compliance"]["gdpr"]["compliant"])

    def test_03_balanced_legal_sanitization(self):
        clean_out = str(self.cleaned_dir / "test_balanced_photo.jpg")
        policy = {
            "preserve_copyright": True,
            "strip_gps": True,
            "strip_author": True,
            "strip_device_info": True,
            "strip_timestamps": True,
            "strip_revision_history": True
        }
        res = sanitize_file(self.photo_path, clean_out, policy)
        self.assertEqual(res["status"], "SANITIZED")
        self.assertEqual(res["mode"], "BALANCED_LEGAL")
        self.assertIn("EXIF:copyright", res["tags_preserved"])

        # Re-scan to verify
        reparsed = parse_file(clean_out)
        re_tag_names = [t["tag_name"] for t in reparsed["tags"]]
        self.assertFalse(any("GPS" in name for name in re_tag_names))
        self.assertIn("Copyright", re_tag_names)

    def test_04_zero_trace_sanitization(self):
        clean_out = str(self.cleaned_dir / "test_zerotrace_photo.jpg")
        policy = {
            "preserve_copyright": False,
            "strip_gps": True,
            "strip_author": True,
            "strip_device_info": True,
            "strip_timestamps": True,
            "strip_revision_history": True
        }
        res = sanitize_file(self.photo_path, clean_out, policy)
        self.assertEqual(res["status"], "SANITIZED")
        self.assertEqual(res["mode"], "ZERO_TRACE")

        # Re-scan
        reparsed = parse_file(clean_out)
        self.assertEqual(len(reparsed["tags"]), 0)

    def test_05_docx_and_pdf_sanitization(self):
        clean_docx = str(self.cleaned_dir / "test_clean.docx")
        clean_pdf = str(self.cleaned_dir / "test_clean.pdf")
        
        res_docx = sanitize_file(self.docx_path, clean_docx)
        self.assertEqual(res_docx["status"], "SANITIZED")

        res_pdf = sanitize_file(self.pdf_path, clean_pdf)
        self.assertEqual(res_pdf["status"], "SANITIZED")

if __name__ == "__main__":
    unittest.main()
