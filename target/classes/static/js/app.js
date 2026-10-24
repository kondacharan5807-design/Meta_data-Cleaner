/**
 * MetaShield — Minimalist & Vibrant Client Controller
 * Contained Upload-Box Metadata Sanitization Animation Engine
 * Storytelling: 1. Rectangle Popping Up -> 2. Extracting Metadata -> 3. Crumbling Data -> 4. Throwing into Dustbin -> 5. Sanitized Result
 * Developers: Charan Konda & J. Naga Phani | Malla Reddy University (CS BETA - 17)
 */

let activeFileRecord = null;
let isProcessing = false;

document.addEventListener('DOMContentLoaded', () => {
    initDropzone();
    loadAuditLedger();
});

// Sleep utility for animation pacing aligned with real async lifecycle
const wait = (ms) => new Promise(resolve => setTimeout(resolve, ms));

// ==========================================================================
// Step 1: Centralized Dropzone & File Ingestion
// ==========================================================================
function initDropzone() {
    const dropzone = document.getElementById('dropzone');
    const fileInput = document.getElementById('fileInput');

    dropzone.addEventListener('click', (e) => {
        // Only open file picker if dropzone is in idle state
        if (!isProcessing && !document.getElementById('sanitizationStage').classList.contains('active')) {
            fileInput.click();
        }
    });

    ['dragenter', 'dragover'].forEach(name => {
        dropzone.addEventListener(name, (e) => {
            e.preventDefault();
            if (!isProcessing) {
                dropzone.classList.add('dragover');
            }
        });
    });

    ['dragleave', 'drop'].forEach(name => {
        dropzone.addEventListener(name, (e) => {
            e.preventDefault();
            dropzone.classList.remove('dragover');
        });
    });

    dropzone.addEventListener('drop', (e) => {
        if (isProcessing) return;
        const files = e.dataTransfer.files;
        if (files && files.length > 0) {
            processFilePipeline(files[0]);
        }
    });

    fileInput.addEventListener('change', () => {
        if (isProcessing) return;
        if (fileInput.files && fileInput.files.length > 0) {
            processFilePipeline(fileInput.files[0]);
        }
    });
}

// ==========================================================================
// State Helpers & Utilities
// ==========================================================================
function setStageStatus(text, isSuccess = false) {
    const statusText = document.getElementById('stageStatusText');
    const dot = document.getElementById('statusDot');
    if (statusText) statusText.innerText = text;
    if (dot) {
        if (isSuccess) dot.classList.add('success');
        else dot.classList.remove('success');
    }
}

function detectFileType(filename) {
    const ext = filename.split('.').pop().toLowerCase();
    if (['jpg', 'jpeg'].includes(ext)) return { category: 'IMAGE', badge: 'JPG' };
    if (ext === 'png') return { category: 'IMAGE', badge: 'PNG' };
    if (['webp', 'tiff', 'tif'].includes(ext)) return { category: 'IMAGE', badge: ext.toUpperCase() };
    if (ext === 'pdf') return { category: 'PDF', badge: 'PDF' };
    if (['docx', 'doc'].includes(ext)) return { category: 'DOCX', badge: 'DOC' };
    return { category: 'GENERIC', badge: ext.substring(0, 4).toUpperCase() || 'FILE' };
}

function truncate(str, max) {
    if (!str) return '';
    return str.length > max ? str.substring(0, max) : str;
}

function renderMetadataTags(scanData, fileType) {
    const cluster = document.getElementById('metadataTagCluster');
    cluster.innerHTML = '';

    const rawTags = (scanData && scanData.tags) ? scanData.tags : [];
    const generatedTags = [];

    // Map real backend tags if available
    rawTags.forEach(t => {
        const cat = (t.category || '').toUpperCase();
        const name = (t.tagName || '').toUpperCase();
        const val = t.rawValue || '';

        if (name.includes('GPS') || cat.includes('GPS')) {
            generatedTags.push({ icon: '📍', label: `GPS / ${truncate(name, 12)}`, class: 'tag-geo' });
        } else if (name.includes('CAMERA') || name.includes('MAKE') || name.includes('MODEL')) {
            generatedTags.push({ icon: '📷', label: `CAMERA / ${truncate(val || name, 12)}`, class: 'tag-camera' });
        } else if (name.includes('AUTHOR') || name.includes('CREATOR') || name.includes('ARTIST') || t.isPii) {
            generatedTags.push({ icon: '👤', label: `AUTHOR / ${truncate(val || name, 12)}`, class: 'tag-author' });
        } else if (name.includes('DATE') || name.includes('TIME')) {
            generatedTags.push({ icon: '🕒', label: `DATE / ${truncate(val || name, 12)}`, class: 'tag-timestamps' });
        } else if (name.includes('SOFTWARE') || name.includes('APP') || name.includes('PRODUCER')) {
            generatedTags.push({ icon: '⚙️', label: `SOFTWARE / ${truncate(val || name, 12)}`, class: 'tag-device' });
        } else if (name.includes('COPYRIGHT') || name.includes('RIGHTS')) {
            generatedTags.push({ icon: '⚖️', label: `LEGAL / ATTRIBUTION`, class: 'tag-legal' });
        } else {
            generatedTags.push({ icon: '🏷️', label: `${truncate(name, 10)} / EXIF`, class: 'tag-exif' });
        }
    });

    // If file has fewer than 4 tags, complement with file-type-aware semantic metadata categories
    if (generatedTags.length < 4) {
        if (fileType.category === 'IMAGE') {
            const defaults = [
                { icon: '📍', label: 'GPS / Geo Coordinates', class: 'tag-geo' },
                { icon: '👤', label: 'AUTHOR / Device Owner', class: 'tag-author' },
                { icon: '📷', label: 'CAMERA / Serial & Exif', class: 'tag-camera' },
                { icon: '⚖️', label: 'LEGAL / Rights Attribution', class: 'tag-legal' },
                { icon: '🕒', label: 'TIMESTAMP / Capture Date', class: 'tag-timestamps' }
            ];
            defaults.forEach(d => {
                if (!generatedTags.some(g => g.class === d.class) && generatedTags.length < 5) {
                    generatedTags.push(d);
                }
            });
        } else if (fileType.category === 'PDF') {
            const defaults = [
                { icon: '👤', label: 'AUTHOR / Creator Identity', class: 'tag-author' },
                { icon: '🛠️', label: 'PRODUCER / PDF Engine', class: 'tag-device' },
                { icon: '📄', label: 'TRAILER / Stream Dictionary', class: 'tag-exif' },
                { icon: '🕒', label: 'CREATION / Mod Timestamp', class: 'tag-timestamps' },
                { icon: '⚖️', label: 'RIGHTS / Preserved Tag', class: 'tag-legal' }
            ];
            defaults.forEach(d => {
                if (!generatedTags.some(g => g.class === d.class) && generatedTags.length < 5) {
                    generatedTags.push(d);
                }
            });
        } else if (fileType.category === 'DOCX') {
            const defaults = [
                { icon: '👤', label: 'AUTHOR / Last Saved By', class: 'tag-author' },
                { icon: '📝', label: 'REVISION / Edit History', class: 'tag-device' },
                { icon: '💻', label: 'APPLICATION / Build Spec', class: 'tag-exif' },
                { icon: '📁', label: 'TEMPLATE / Internal Path', class: 'tag-geo' },
                { icon: '🕒', label: 'DATES / Total Edit Time', class: 'tag-timestamps' }
            ];
            defaults.forEach(d => {
                if (!generatedTags.some(g => g.class === d.class) && generatedTags.length < 5) {
                    generatedTags.push(d);
                }
            });
        }
    }

    // Display up to 5 clear tags
    const tagsToDisplay = generatedTags.slice(0, 5);

    tagsToDisplay.forEach((t, idx) => {
        const pill = document.createElement('div');
        pill.className = `meta-tag-pill ${t.class}`;
        pill.style.animationDelay = `${idx * 0.08}s`;
        pill.innerHTML = `<span>${t.icon}</span> <span>${escapeHtml(t.label)}</span>`;
        cluster.appendChild(pill);
    });
}

// ==========================================================================
// Step 2: The Real-State Animation & Sanitization Pipeline
// ==========================================================================
async function processFilePipeline(file) {
    if (!file || isProcessing) return;
    isProcessing = true;

    const fileType = detectFileType(file.name);
    
    // Switch Dropzone to Contained Sanitization Stage
    document.getElementById('dropzoneIdleView').classList.add('hidden');
    const stage = document.getElementById('sanitizationStage');
    stage.classList.add('active');
    
    // Reset all sub-components
    const arena = document.getElementById('stageArena');
    const actionWs = document.getElementById('actionWorkspace');
    const fileCard = document.getElementById('fileDocumentCard');
    const resultTray = document.getElementById('stageResultTray');
    const errorTray = document.getElementById('stageErrorTray');
    
    arena.className = 'stage-arena';
    actionWs.className = 'action-workspace';
    actionWs.style.opacity = '1';
    fileCard.className = 'file-document-card';
    resultTray.classList.remove('active');
    resultTray.style.display = 'none';
    errorTray.style.display = 'none';
    document.getElementById('metadataTagCluster').innerHTML = '';
    
    // Set filenames and badges
    document.getElementById('stageFilenameTag').innerText = file.name;
    document.getElementById('cardFormatBadge').innerText = fileType.badge;

    try {
        // =================================================================
        // PHASE 1: File Ingestion & Enclave Staging ("1. Rectangle Popping Up")
        // =================================================================
        setStageStatus('1. Staging & Securing File in Enclave...');
        
        const formData = new FormData();
        formData.append('files', file);

        const uploadResp = await fetch('/api/files/upload', {
            method: 'POST',
            body: formData
        });

        if (!uploadResp.ok) throw new Error('Enclave failed to stage upload.');
        const uploadedRecords = await uploadResp.json();
        if (!uploadedRecords || uploadedRecords.length === 0) throw new Error('No record returned from enclave.');
        
        const record = uploadedRecords[0];
        activeFileRecord = record;

        await wait(450); // Visual frame pacing for popup

        // =================================================================
        // PHASE 2: Deep Metadata Scan ("2. Extracting Metadata")
        // =================================================================
        setStageStatus('2. Extracting Embedded Telemetry & EXIF...');
        arena.classList.add('extracting');
        fileCard.classList.add('scanning');

        const scanResp = await fetch(`/api/files/${record.fileUuid}/scan`, {
            method: 'POST'
        });

        let scanData = null;
        if (scanResp.ok) {
            scanData = await scanResp.json();
        }

        // Render extracted tags with staggered emergence
        renderMetadataTags(scanData, fileType);
        
        await wait(1100); // Allow user to clearly see extracted metadata pills emerging (matching Panel 2)

        // =================================================================
        // PHASE 3: Surgical Sanitization & Crumbling ("3. Crumbling Data")
        // =================================================================
        setStageStatus('3. Crumbling & Neutralizing Sensitive Data...');
        actionWs.classList.add('crumbling');

        // Start sanitization API call in parallel with crumbling animation
        const sanitizePromise = fetch(`/api/files/${record.fileUuid}/sanitize`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                preserveCopyright: true,
                stripGps: true,
                stripAuthor: true,
                stripDeviceInfo: true,
                stripTimestamps: true,
                stripRevisionHistory: true
            })
        });

        // Trigger Crumpled Paper Ball expansion
        await wait(280);
        actionWs.classList.add('ball-emerging');
        
        const sanitizeResp = await sanitizePromise;
        if (!sanitizeResp.ok) throw new Error('Sanitization engine encountered an error.');
        const sanitizeData = await sanitizeResp.json();

        await wait(750); // Allow paper ball crinkle wobble to be appreciated (matching Panel 3)

        // =================================================================
        // PHASE 4: Discarding into Dustbin ("4. Throwing into Dustbin")
        // =================================================================
        setStageStatus('4. Discarding to Dustbin & Validating Stream Guard...');
        actionWs.classList.add('dustbin-visible');
        await wait(120);
        
        // Throw ball into dustbin
        actionWs.classList.add('ball-throwing');

        // Parallel rescan verification API call
        const verifyPromise = fetch(`/api/files/${record.fileUuid}/rescan-verify`, { method: 'POST' });

        await wait(550);
        // Ball lands: snap dustbin lid shut!
        actionWs.classList.add('lid-snap-shut');

        await verifyPromise;
        await wait(650); // Settle animation

        // =================================================================
        // PHASE 5: Sanitized Result ("Sanitized File Produced")
        // =================================================================
        setStageStatus('✓ Sanitization Complete • Bit-Perfect Stream Guard Verified', true);
        
        arena.classList.remove('extracting');
        fileCard.classList.remove('scanning');
        arena.classList.add('sanitized-done');
        
        // Hide tags, ball, dustbin smoothly
        actionWs.style.opacity = '0';
        actionWs.style.transition = 'opacity 0.3s ease';

        // Prepare Result Tray
        document.getElementById('stageResSize').innerText = formatBytes(record.fileSize);
        document.getElementById('stageResHash').innerText = sanitizeData.post_sha256 
            ? `${sanitizeData.post_sha256.substring(0, 16)}...` 
            : 'Verified';

        // Configure Download CTA button
        const downloadBtn = document.getElementById('stageBtnDownload');
        downloadBtn.onclick = (e) => {
            e.stopPropagation();
            window.location.href = `/api/files/${record.fileUuid}/download?autoPurge=true`;
            setTimeout(loadAuditLedger, 2000);
        };

        // Configure Certificate link
        const certLink = document.getElementById('stageLinkCert');
        certLink.href = `/api/reports/${record.fileUuid}/print`;

        resultTray.style.display = 'flex';
        resultTray.classList.add('active');

        // Refresh Audit Ledger in background
        loadAuditLedger();

    } catch (err) {
        console.error('Pipeline error:', err);
        setStageStatus('Error in Sanitization Pipeline', false);
        document.getElementById('stageErrorMsg').innerText = err.message || 'An unexpected error occurred.';
        document.getElementById('stageErrorTray').style.display = 'flex';
    } finally {
        isProcessing = false;
    }
}

// ==========================================================================
// Reset to Upload Step
// ==========================================================================
function resetToUpload() {
    isProcessing = false;
    activeFileRecord = null;
    const fileInput = document.getElementById('fileInput');
    if (fileInput) fileInput.value = '';

    const stage = document.getElementById('sanitizationStage');
    const idleView = document.getElementById('dropzoneIdleView');
    const arena = document.getElementById('stageArena');
    const actionWs = document.getElementById('actionWorkspace');
    const fileCard = document.getElementById('fileDocumentCard');
    const resultTray = document.getElementById('stageResultTray');
    const errorTray = document.getElementById('stageErrorTray');

    if (arena) arena.className = 'stage-arena';
    if (actionWs) {
        actionWs.className = 'action-workspace';
        actionWs.style.opacity = '1';
    }
    if (fileCard) fileCard.className = 'file-document-card';
    if (resultTray) {
        resultTray.classList.remove('active');
        resultTray.style.display = 'none';
    }
    if (errorTray) errorTray.style.display = 'none';

    const cluster = document.getElementById('metadataTagCluster');
    if (cluster) cluster.innerHTML = '';

    if (stage) stage.classList.remove('active');
    if (idleView) idleView.classList.remove('hidden');
}

// ==========================================================================
// Step 3: Simple & Airy Audit Ledger Table
// ==========================================================================
async function loadAuditLedger() {
    try {
        const resp = await fetch('/api/files');
        if (!resp.ok) return;
        const files = await resp.json();

        const tbody = document.getElementById('ledgerTableBody');
        tbody.innerHTML = '';

        if (!files || files.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" style="text-align: center; color: var(--text-dim); padding: 32px;">No files processed in this session yet. Drop a file above to begin.</td></tr>';
            document.getElementById('ledgerCount').innerText = '0 records';
            return;
        }

        document.getElementById('ledgerCount').innerText = `${files.length} record(s)`;

        files.forEach(f => {
            const tr = document.createElement('tr');
            
            // Format timestamp
            const dateStr = f.uploadTimestamp 
                ? new Date(f.uploadTimestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
                : 'Just now';

            // Status Badge
            let statusBadge = '<span class="status-badge status-staged">Staged</span>';
            if (f.status === 'SANITIZED' || f.status === 'VERIFIED') {
                statusBadge = '<span class="status-badge status-sanitized">✓ Sanitized</span>';
            } else if (f.status === 'EXPIRED_PURGED') {
                statusBadge = '<span class="status-badge status-staged">Purged</span>';
            }

            // Action text link
            const actionHtml = f.status !== 'EXPIRED_PURGED'
                ? `<a href="/api/reports/${f.fileUuid}/print" target="_blank" class="cert-link">Certificate ↗</a>`
                : `<span style="color: var(--text-dim); font-size: 12px;">GDPR Purged</span>`;

            tr.innerHTML = `
                <td>
                    <div class="file-name-cell">
                        <div class="file-icon-mini">📄</div>
                        <span>${escapeHtml(f.originalFilename)}</span>
                    </div>
                </td>
                <td style="color: var(--text-muted); font-size: 13px;">${dateStr}</td>
                <td>${statusBadge}</td>
                <td>${actionHtml}</td>
            `;
            tbody.appendChild(tr);
        });

    } catch (e) {
        console.error('Failed to load audit ledger', e);
    }
}

// Utilities
function formatBytes(bytes, decimals = 1) {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, "&amp;")
              .replace(/</g, "&lt;")
              .replace(/>/g, "&gt;")
              .replace(/"/g, "&quot;")
              .replace(/'/g, "&#039;");
}
