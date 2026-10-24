# MetaShield — Intelligent Privacy Framework for Metadata Security and Risk-Based Removal

![MetaShield Banner](src/main/resources/static/assets/banner.jpg)

## 📌 Project Overview
**MetaShield** is an advanced, enterprise privacy framework engineered to detect, analyze, and eliminate hidden metadata from digital files (images, documents, and PDFs) prior to sharing or public publication. By intercepting embedded telemetry—including precise GPS coordinates, device serial numbers, author credentials, edit timestamps, and hidden revision histories—MetaShield neutralizes accidental data exposure, corporate espionage, and regulatory non-compliance.

### 🎓 Academic Ownership & Attribution
* **Institution:** Malla Reddy University, Hyderabad
* **Department:** Computer Science & Engineering
* **Batch:** CS BETA – 17
* **Lead Architects:** 
  * **Charan Konda** (Roll No: `2511CS040113`)
  * **J. Naga Phani** (Roll No: `2511CS040132`)

---

## 🏗️ Technology Stack & Separation of Concerns

| Layer | Technology | Key Responsibility |
|---|---|---|
| **Database (Strict)** | **SQLite 3 (WAL Mode)** | Immutable, cryptographically chained audit ledger, file states, and policy store. Dedicated single-threaded write queue (`SqliteWriteQueue`) eliminates `SQLITE_BUSY` bottlenecks. |
| **Backend Orchestration** | **Java 21 / Spring Boot 3.3** | REST API gateway, batch upload staging, persistent daemon lifecycle supervision, scheduled 1-hour GDPR session TTL eviction. |
| **Sanitization & Parsing Engine** | **Python 3.14 (Persistent Daemon)** | Deep EXIF/GPS extraction, XMP blocks, PDF trailer inspection, OOXML revisions, surgical selective wiping. |
| **Anti-Corruption Binary Guard** | **C++ (MinGW g++)** | High-performance binary stream validator checking magic bytes, chunk integrity (JPEG SOI/EOI, PNG IEND CRC, PDF %EOF), and computing SHA-256 digests. |
| **Frontend Dashboard** | **HTML5, CSS3, ES6 JavaScript** | High-density cybersecurity telemetry console inspired by Stitch MCP specifications, featuring animated SVG risk gauges, drag-and-drop batch upload, and audit verification. |
| **Cloud Infrastructure** | **Google Cloud Platform (GCP)** | Shielded VMs (Secure Boot, vTPM, Integrity Monitoring), 100% VPC Flow Logs, and Cloud Backup & DR immutable retention policies. |

---

## 🔒 Regulatory Compliance Alignment
* **GDPR (Articles 5 & 25):** Enforces Data Minimisation and Storage Limitation. Raw staging files auto-purge after a 1-hour session TTL or immediately upon user download.
* **CCPA (§ 1798.140):** Detects and eradicates Sensitive Personal Information (SPI) such as precise geolocation coordinates.
* **HIPAA (Safe Harbor 45 CFR § 164.514):** Automatically redacts author names, system paths, and device identifiers in document metadata.
* **ISO/IEC 27001 (Control A.8.11):** Non-repudiable audit trails verified via SHA-256 chained block hashes in SQLite.

---

## 🚀 Quickstart Guide (100% Portable Across Any System)

### 1. Prerequisites
* **Node.js 18+** (for the Vite + React Frontend & Portable Orchestrator)
* **Java 21+** (Auto-detected across system PATH and standard JDK folders)
* **Python 3.10+** (Auto-detected; missing libraries `pillow`, `pypdf`, `python-docx`, `lxml` are automatically installed on startup)

### 2. Start the Entire Application in a Single Go

#### Option A: Using Node (`npm run dev`)
```powershell
npm install   # (Only needed the first time if node_modules is missing)
npm run dev
```

#### Option B: Using PowerShell (`start.ps1`)
```powershell
.\start.ps1
```
*(Automatically unblocks downloaded zip files, installs Node/Python packages if missing, starts the Spring Boot + Python/C++ Backend on port `8080`, and launches the Vite + React Frontend on port `5174`.)*

#### Option C: One-Click Windows Batch (`start.bat`)
Double-click `start.bat` or run:
```cmd
start.bat
```

### 3. Access the Application
Open your browser and navigate to:
* **Vite + React Frontend:** [http://localhost:5174](http://localhost:5174)
* **Spring Boot Backend API:** [http://localhost:8080](http://localhost:8080)

---

## 🧪 Testing & Verification
Run the Python engine unit tests:
```powershell
python -m unittest engine/test_samples/test_engine.py
```

Verify C++ stream guard independently:
```powershell
.\engine\stream_guard.exe --verify engine/test_samples/sample_field_photo.jpg
```
