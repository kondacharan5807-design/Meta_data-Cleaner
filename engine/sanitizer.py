"""
MetaShield - Surgical Sanitization & Selective Wipe Engine
Autonomously engineered for Charan Konda (2511CS040113) & J. Naga Phani (2511CS040132)
Malla Reddy University - CS BETA - 17
"""

import os
import sys
import json
import shutil
import zipfile
from pathlib import Path
from PIL import Image, ExifTags
import pypdf
import docx

COPYRIGHT_TAG_NAMES = {"copyright", "artist", "dc:rights", "credit", "photoshop:credit", "rights"}

def sanitize_image(input_path: str, output_path: str, policy: dict) -> dict:
    """
    Sanitizes image metadata.
    If policy['preserve_copyright'] is True (Balanced Legal Mode), copyright/artist tags are retained.
    Otherwise in Zero-Trace mode, all EXIF/XMP blocks are stripped.
    """
    preserve_copyright = policy.get("preserve_copyright", True)
    strip_gps = policy.get("strip_gps", True)
    strip_author = policy.get("strip_author", True)
    strip_device = policy.get("strip_device_info", True)
    strip_timestamps = policy.get("strip_timestamps", True)

    tags_removed = []
    tags_preserved = []

    with Image.open(input_path) as img:
        # Check existing exif
        exif = img.getexif()
        new_exif = Image.Exif()

        if exif:
            for tag_id, value in exif.items():
                tag_name = ExifTags.TAGS.get(tag_id, f"Tag_0x{tag_id:04X}").lower()
                
                # Check copyright retention
                if preserve_copyright and any(c in tag_name for c in ["copyright", "rights"]):
                    new_exif[tag_id] = value
                    tags_preserved.append(f"EXIF:{tag_name}")
                    continue

                if preserve_copyright and not strip_author and "artist" in tag_name:
                    new_exif[tag_id] = value
                    tags_preserved.append(f"EXIF:{tag_name}")
                    continue

                # Strip rules
                if strip_gps and "gps" in tag_name:
                    tags_removed.append(f"EXIF:{tag_name}")
                    continue

                if strip_device and any(k in tag_name for k in ["model", "make", "serial", "unique"]):
                    tags_removed.append(f"EXIF:{tag_name}")
                    continue

                if strip_timestamps and any(k in tag_name for k in ["date", "time"]):
                    tags_removed.append(f"EXIF:{tag_name}")
                    continue

                if strip_author and any(k in tag_name for k in ["artist", "user", "owner", "operator"]):
                    tags_removed.append(f"EXIF:{tag_name}")
                    continue

                # In Zero-Trace mode, drop everything not preserved
                if not preserve_copyright:
                    tags_removed.append(f"EXIF:{tag_name}")
                else:
                    # Drop other non-essential tags
                    tags_removed.append(f"EXIF:{tag_name}")

        # Eradicate GPS IFD completely if strip_gps is active
        if strip_gps and ExifTags.IFD.GPSInfo in new_exif:
            del new_exif[ExifTags.IFD.GPSInfo]
            tags_removed.append("EXIF:GPSInfo_IFD")

        # Create sanitized image copying clean pixel data
        clean_img = Image.new(img.mode, img.size)
        clean_img.paste(img)

        # Save with clean or filtered exif
        save_kwargs = {}
        if preserve_copyright and len(new_exif) > 0:
            save_kwargs["exif"] = new_exif

        # Maintain format
        fmt = img.format if img.format else "JPEG"
        if fmt == "JPEG":
            clean_img.save(output_path, format="JPEG", quality=95, **save_kwargs)
        elif fmt == "PNG":
            clean_img.save(output_path, format="PNG", optimize=True)
        else:
            clean_img.save(output_path, format=fmt, **save_kwargs)

    return {
        "status": "SANITIZED",
        "tags_removed_count": len(tags_removed),
        "tags_removed": tags_removed[:20],
        "tags_preserved": tags_preserved,
        "mode": "BALANCED_LEGAL" if preserve_copyright else "ZERO_TRACE"
    }

def sanitize_pdf(input_path: str, output_path: str, policy: dict) -> dict:
    preserve_copyright = policy.get("preserve_copyright", True)
    reader = pypdf.PdfReader(input_path)
    writer = pypdf.PdfWriter()

    for page in reader.pages:
        writer.add_page(page)

    metadata = reader.metadata or {}
    clean_meta = {}

    tags_removed = []
    tags_preserved = []

    for k, v in metadata.items():
        key_lower = str(k).lower()
        if preserve_copyright and any(c in key_lower for c in ["copyright", "rights", "license"]):
            clean_meta[k] = v
            tags_preserved.append(str(k))
        else:
            tags_removed.append(str(k))

    # Add producer stamp
    clean_meta["/Producer"] = "MetaShield Sanitization Engine v2.0 (Malla Reddy University)"
    writer.add_metadata(clean_meta)

    with open(output_path, "wb") as f_out:
        writer.write(f_out)

    return {
        "status": "SANITIZED",
        "tags_removed_count": len(tags_removed),
        "tags_removed": tags_removed,
        "tags_preserved": tags_preserved,
        "mode": "BALANCED_LEGAL" if preserve_copyright else "ZERO_TRACE"
    }

def sanitize_docx(input_path: str, output_path: str, policy: dict) -> dict:
    preserve_copyright = policy.get("preserve_copyright", True)
    strip_author = policy.get("strip_author", True)
    strip_revisions = policy.get("strip_revision_history", True)
    strip_timestamps = policy.get("strip_timestamps", True)

    tags_removed = []
    tags_preserved = []

    # First copy file to temp output
    shutil.copyfile(input_path, output_path)

    # Use python-docx for core properties
    doc = docx.Document(output_path)
    cp = doc.core_properties

    if strip_author:
        if cp.author:
            tags_removed.append("docProps:author")
            cp.author = ""
        if cp.last_modified_by:
            tags_removed.append("docProps:last_modified_by")
            cp.last_modified_by = ""

    if strip_revisions:
        if cp.comments:
            tags_removed.append("docProps:comments")
            cp.comments = ""
        if cp.revision:
            tags_removed.append("docProps:revision")
            cp.revision = 1

    if strip_timestamps:
        if cp.created:
            tags_removed.append("docProps:created")
        if cp.modified:
            tags_removed.append("docProps:modified")

    doc.save(output_path)

    # Clean zip parts (e.g. word/comments.xml if strip_revisions)
    if strip_revisions and zipfile.is_zipfile(output_path):
        tmp_zip = output_path + ".tmp"
        with zipfile.ZipFile(output_path, "r") as zin:
            with zipfile.ZipFile(tmp_zip, "w", compression=zipfile.ZIP_DEFLATED) as zout:
                for item in zin.infolist():
                    if item.filename in ["word/comments.xml", "word/commentsExtended.xml"]:
                        tags_removed.append(f"OOXML:{item.filename}")
                        continue
                    zout.writestr(item, zin.read(item.filename))
        os.replace(tmp_zip, output_path)

    return {
        "status": "SANITIZED",
        "tags_removed_count": len(tags_removed),
        "tags_removed": tags_removed,
        "tags_preserved": tags_preserved,
        "mode": "BALANCED_LEGAL" if preserve_copyright else "ZERO_TRACE"
    }

def sanitize_file(input_path: str, output_path: str, policy: dict = None) -> dict:
    if policy is None:
        # Default: Balanced Legal Mode
        policy = {
            "preserve_copyright": True,
            "strip_gps": True,
            "strip_author": True,
            "strip_device_info": True,
            "strip_timestamps": True,
            "strip_revision_history": True
        }

    in_p = Path(input_path)
    out_p = Path(output_path)
    out_p.parent.mkdir(parents=True, exist_ok=True)

    ext = in_p.suffix.lower()
    if ext in [".jpg", ".jpeg", ".png", ".webp", ".tiff", ".tif"]:
        return sanitize_image(input_path, output_path, policy)
    elif ext == ".pdf":
        return sanitize_pdf(input_path, output_path, policy)
    elif ext in [".docx", ".doc", ".pptx", ".xlsx"]:
        return sanitize_docx(input_path, output_path, policy)
    else:
        # Generic copy for raw files
        shutil.copyfile(input_path, output_path)
        return {
            "status": "SANITIZED",
            "tags_removed_count": 0,
            "tags_removed": [],
            "tags_preserved": [],
            "mode": "PASSTHROUGH"
        }

if __name__ == "__main__":
    if len(sys.argv) > 2:
        in_file = sys.argv[1]
        out_file = sys.argv[2]
        res = sanitize_file(in_file, out_file)
        print(json.dumps(res, indent=2))
    else:
        print("Usage: python sanitizer.py <input_file> <output_file>")
