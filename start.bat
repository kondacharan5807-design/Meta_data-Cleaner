@echo off
cd /d "%~dp0"
title MetaShield — Vite + React Full-Stack Server
echo ========================================================
echo        MetaShield - Privacy ^& Metadata Sanitizer
echo   Malla Reddy University ^| CS BETA - 17
echo   Lead Architects: Charan Konda ^& J. Naga Phani
echo ========================================================
echo.

if not exist "node_modules\vite\bin\vite.js" (
    echo [*] First-time setup: Installing Vite + React dependencies...
    call npm install
)

echo [*] Starting MetaShield Full-Stack Server on http://localhost:5174 ...
call npm run dev
pause
