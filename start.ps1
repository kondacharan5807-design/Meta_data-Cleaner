# ==============================================================================
# MetaShield — Universal Portable One-Click Startup Script (PowerShell)
# Starts the Spring Boot + Python/C++ Backend AND Vite + React Frontend (Port 5174)
# Fully portable across any system when zipped and shared.
# Developers: Charan Konda & J. Naga Phani | Malla Reddy University (CS BETA - 17)
# ==============================================================================

$ErrorActionPreference = "Continue"
Set-Location -Path $PSScriptRoot

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "       MetaShield - Privacy & Metadata Sanitizer" -ForegroundColor Green
Write-Host "  Malla Reddy University | CS BETA - 17" -ForegroundColor Yellow
Write-Host "  Lead Architects: Charan Konda & J. Naga Phani" -ForegroundColor Yellow
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Unblock files if extracted from a downloaded .zip archive on Windows
Write-Host "[*] Preparing portable workspace..." -ForegroundColor DarkGray
Get-ChildItem -Path $PSScriptRoot -Recurse -ErrorAction SilentlyContinue | Unblock-File -ErrorAction SilentlyContinue

# 2. Verify Node.js & npm are available
if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    Write-Host "[!] Node.js is not found in PATH. Please install Node.js (https://nodejs.org/) to run Vite + React." -ForegroundColor Red
    exit 1
}

# 3. Auto-install Node dependencies if node_modules is missing (e.g., on a friend's machine after unzipping)
$ViteBin = Join-Path $PSScriptRoot "node_modules\vite\bin\vite.js"
if (-not (Test-Path $ViteBin)) {
    Write-Host "[*] First-time setup detected: Installing Vite + React dependencies (npm install)..." -ForegroundColor Yellow
    npm install
}

# 4. Launch the complete application (Backend on :8080 + Vite React Frontend on :5174)
Write-Host "[*] Launching MetaShield Full-Stack Application (Frontend -> http://localhost:5174)..." -ForegroundColor Green
npm run dev