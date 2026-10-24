"""
MetaShield - Metadata Intelligence & Parsing Engine
Autonomously engineered for Charan Konda (2511CS040113) & J. Naga Phani (2511CS040132)
Malla Reddy University - CS BETA - 17
"""

import os
import sys
import json
import xml.etree.ElementTree as ET
from pathlib import Path
from PIL import Image, ExifTags
import pypdf
import docx
import zipfile

def _get_field_severity(tag_name: str, category: str, value: str) -> tuple[str, int, bool]:
    """
    Returns (severity_level, risk_weight, is_pii)
    Levels: CRITICAL, HIGH, MEDIUM, LOW, SAFE
    """
    t_lower = tag_name.lower()
    c_lower = category.lower()

    if "gps" in t_lower or "gps" in c_lower or "latitude" in t_lower or "longitude" in t_lower or "altitude" in t_lower:
        return "CRITICAL", 35, True
    
    if any(k in t_lower for k in ["serial", "device", "mac", "uniqueid", "imei", "uuid"]):
        return "HIGH", 20, True

    if any(k in t_lower for k in ["author", "creator", "lastmodifiedby", "operator", "user", "owner", "contributor"]):
        return "HIGH", 25, True

    if any(k in t_lower for k in ["revision", "version", "history", "trackedchanges", "comment"]):
        return "HIGH", 15, False

    if any(k in t_lower for k in ["date", "time", "created", "modified", "timestamp"]):
        return "MEDIUM", 10, False

    if any(k in t_lower for k in ["software", "producer", "app", "os", "platform"]):
        return "MEDIUM", 8, False

    if any(k in t_lower for k in ["copyright", "rights", "license", "attribution", "credit"]):
        return "SAFE", 0, False

    return "LOW", 3, False

def parse_image(file_path: str) -> dict:
    tags = []
    try:
        with Image.open(file_path) as img:
            raw_exif = img.getexif()
            if raw_exif:
                for tag_id, value in raw_exif.items():
                    tag_name = ExifTags.TAGS.get(tag_id, f"Tag_0x{tag_id:04X}")
                    val_str = str(value)
                    # Skip massive raw bytes
                    if len(val_str) > 300:
                        val_str = val_str[:297] + "..."
                    sev, weight, pii = _get_field_severity(tag_name, "EXIF", val_str)
                    tags.append({
                        "category": "EXIF",
                        "tag_name": tag_name,
                        "raw_value": val_str,
                        "severity": sev,
                        "weight": weight,
                        "is_pii": pii
                    })

                # Check GPS IFD specifically
                gps_ifd = raw_exif.get_ifd(ExifTags.IFD.GPSInfo)
                if gps_ifd:
                    for gps_id, gps_val in gps_ifd.items():
                        gps_tag = ExifTags.GPSTAGS.get(gps_id, f"GPS_0x{gps_id:04X}")
                        val_str = str(gps_val)
                        if len(val_str) > 300:
                            val_str = val_str[:297] + "..."
                        tags.append({
                            "category": "GPS",
                            "tag_name": f"GPS_{gps_tag}",
                            "raw_value": val_str,
                            "severity": "CRITICAL",
                            "weight": 35,
                            "is_pii": True
                        })

            # Check XMP metadata info if available
            if hasattr(img, "info") and "xmp" in img.info:
                xmp_data = img.info["xmp"]
                if isinstance(xmp_data, bytes):
                    try:
                        xmp_str = xmp_data.decode("utf-8", errors="ignore")
                        tags.append({
                            "category": "XMP",
                            "tag_name": "Embedded_XMP_Packet",
                            "raw_value": f"XMP Payload ({len(xmp_str)} bytes)",
                            "severity": "MEDIUM",
                            "weight": 10,
                            "is_pii": False
                        })
                    except Exception:
                        pass
    except Exception as e:
        tags.append({
            "category": "SYSTEM",
            "tag_name": "Parsing_Notice",
            "raw_value": f"Standard image headers read with notice: {str(e)}",
            "severity": "LOW",
            "weight": 0,
            "is_pii": False
        })
    return {"format": "IMAGE", "tags": tags}

def parse_pdf(file_path: str) -> dict:
    tags = []
    try:
        reader = pypdf.PdfReader(file_path)
        metadata = reader.metadata
        if metadata:
            for k, v in metadata.items():
                clean_key = str(k).lstrip("/")
                val_str = str(v)
                sev, weight, pii = _get_field_severity(clean_key, "PDFInfo", val_str)
                tags.append({
                    "category": "PDFInfo",
                    "tag_name": clean_key,
                    "raw_value": val_str,
                    "severity": sev,
                    "weight": weight,
                    "is_pii": pii
                })
        
        # Check XMP metadata
        if reader.xmp_metadata:
            for prop in ["dc_creator", "dc_description", "dc_rights", "xmp_create_date", "xmp_modify_date"]:
                val = getattr(reader.xmp_metadata, prop, None)
                if val:
                    sev, weight, pii = _get_field_severity(prop, "XMP", str(val))
                    tags.append({
                        "category": "XMP",
                        "tag_name": f"XMP_{prop}",
                        "raw_value": str(val),
                        "severity": sev,
                        "weight": weight,
                        "is_pii": pii
                    })
    except Exception as e:
        tags.append({
            "category": "SYSTEM",
            "tag_name": "PDF_Read_Notice",
            "raw_value": str(e),
            "severity": "LOW",
            "weight": 0,
            "is_pii": False
        })
    return {"format": "PDF", "tags": tags}

def parse_docx(file_path: str) -> dict:
    tags = []
    try:
        doc = docx.Document(file_path)
        cp = doc.core_properties
        core_attrs = [
            ("author", cp.author),
            ("last_modified_by", cp.last_modified_by),
            ("created", cp.created),
            ("modified", cp.modified),
            ("title", cp.title),
            ("subject", cp.subject),
            ("category", cp.category),
            ("comments", cp.comments),
            ("revision", cp.revision),
            ("version", cp.version)
        ]
        for name, val in core_attrs:
            if val is not None and str(val).strip() != "":
                val_str = str(val)
                sev, weight, pii = _get_field_severity(name, "DocumentCore", val_str)
                tags.append({
                    "category": "DocumentCore",
                    "tag_name": name,
                    "raw_value": val_str,
                    "severity": sev,
                    "weight": weight,
                    "is_pii": pii
                })

        # Check for tracked revisions / comments inside package
        if zipfile.is_zipfile(file_path):
            with zipfile.ZipFile(file_path, "r") as z:
                namelist = z.namelist()
                if "word/comments.xml" in namelist:
                    tags.append({
                        "category": "RevisionHistory",
                        "tag_name": "Embedded_Comments",
                        "raw_value": "word/comments.xml present in archive",
                        "severity": "HIGH",
                        "weight": 15,
                        "is_pii": True
                    })
                if "docProps/app.xml" in namelist:
                    app_xml = z.read("docProps/app.xml").decode("utf-8", errors="ignore")
                    if "<Application>" in app_xml:
                        app_name = app_xml.split("<Application>")[1].split("</Application>")[0]
                        tags.append({
                            "category": "System",
                            "tag_name": "ApplicationSignature",
                            "raw_value": app_name,
                            "severity": "MEDIUM",
                            "weight": 8,
                            "is_pii": False
                        })
    except Exception as e:
        tags.append({
            "category": "SYSTEM",
            "tag_name": "DOCX_Read_Notice",
            "raw_value": str(e),
            "severity": "LOW",
            "weight": 0,
            "is_pii": False
        })
    return {"format": "DOCX", "tags": tags}

def parse_file(file_path: str) -> dict:
    path = Path(file_path)
    if not path.exists():
        raise FileNotFoundError(f"Target file not found: {file_path}")

    ext = path.suffix.lower()
    if ext in [".jpg", ".jpeg", ".png", ".webp", ".tiff", ".tif"]:
        return parse_image(file_path)
    elif ext == ".pdf":
        return parse_pdf(file_path)
    elif ext in [".docx", ".doc", ".pptx", ".xlsx"]:
        return parse_docx(file_path)
    else:
        # Fallback binary inspection
        return {
            "format": "BINARY",
            "tags": [{
                "category": "SYSTEM",
                "tag_name": "FileExtension",
                "raw_value": ext,
                "severity": "LOW",
                "weight": 0,
                "is_pii": False
            }]
        }

if __name__ == "__main__":
    if len(sys.argv) > 1:
        res = parse_file(sys.argv[1])
        print(json.dumps(res, indent=2))
    else:
        print("Usage: python parser.py <file_path>")
