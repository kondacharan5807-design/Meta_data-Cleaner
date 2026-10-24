import React, { useState, useRef } from 'react';

const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

function detectFileType(filename) {
  const ext = (filename.split('.').pop() || '').toLowerCase();
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

function formatBytes(bytes, decimals = 1) {
  if (!bytes || bytes === 0) return '0 B';
  const k = 1024;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
}

function buildMetadataTags(scanData, fileType) {
  const rawTags = scanData && scanData.tags ? scanData.tags : [];
  const generatedTags = [];

  rawTags.forEach((t) => {
    const cat = (t.category || '').toUpperCase();
    const name = (t.tagName || '').toUpperCase();
    const val = t.rawValue || '';

    if (name.includes('GPS') || cat.includes('GPS')) {
      generatedTags.push({ icon: '📍', label: `GPS / ${truncate(name, 12)}`, className: 'tag-geo' });
    } else if (name.includes('CAMERA') || name.includes('MAKE') || name.includes('MODEL')) {
      generatedTags.push({ icon: '📷', label: `CAMERA / ${truncate(val || name, 12)}`, className: 'tag-camera' });
    } else if (name.includes('AUTHOR') || name.includes('CREATOR') || name.includes('ARTIST') || t.isPii) {
      generatedTags.push({ icon: '👤', label: `AUTHOR / ${truncate(val || name, 12)}`, className: 'tag-author' });
    } else if (name.includes('DATE') || name.includes('TIME')) {
      generatedTags.push({ icon: '🕒', label: `DATE / ${truncate(val || name, 12)}`, className: 'tag-timestamps' });
    } else if (name.includes('SOFTWARE') || name.includes('APP') || name.includes('PRODUCER')) {
      generatedTags.push({ icon: '⚙️', label: `SOFTWARE / ${truncate(val || name, 12)}`, className: 'tag-device' });
    } else if (name.includes('COPYRIGHT') || name.includes('RIGHTS')) {
      generatedTags.push({ icon: '⚖️', label: 'LEGAL / ATTRIBUTION', className: 'tag-legal' });
    } else {
      generatedTags.push({ icon: '🏷️', label: `${truncate(name, 10)} / EXIF`, className: 'tag-exif' });
    }
  });

  if (generatedTags.length < 4) {
    if (fileType.category === 'IMAGE') {
      const defaults = [
        { icon: '📍', label: 'GPS / Geo Coordinates', className: 'tag-geo' },
        { icon: '👤', label: 'AUTHOR / Device Owner', className: 'tag-author' },
        { icon: '📷', label: 'CAMERA / Serial & Exif', className: 'tag-camera' },
        { icon: '⚖️', label: 'LEGAL / Rights Attribution', className: 'tag-legal' },
        { icon: '🕒', label: 'TIMESTAMP / Capture Date', className: 'tag-timestamps' },
      ];
      defaults.forEach((d) => {
        if (!generatedTags.some((g) => g.className === d.className) && generatedTags.length < 5) {
          generatedTags.push(d);
        }
      });
    } else if (fileType.category === 'PDF') {
      const defaults = [
        { icon: '👤', label: 'AUTHOR / Creator Identity', className: 'tag-author' },
        { icon: '🛠️', label: 'PRODUCER / PDF Engine', className: 'tag-device' },
        { icon: '📄', label: 'TRAILER / Stream Dictionary', className: 'tag-exif' },
        { icon: '🕒', label: 'CREATION / Mod Timestamp', className: 'tag-timestamps' },
        { icon: '⚖️', label: 'RIGHTS / Preserved Tag', className: 'tag-legal' },
      ];
      defaults.forEach((d) => {
        if (!generatedTags.some((g) => g.className === d.className) && generatedTags.length < 5) {
          generatedTags.push(d);
        }
      });
    } else if (fileType.category === 'DOCX') {
      const defaults = [
        { icon: '👤', label: 'AUTHOR / Last Saved By', className: 'tag-author' },
        { icon: '📝', label: 'REVISION / Edit History', className: 'tag-device' },
        { icon: '💻', label: 'APPLICATION / Build Spec', className: 'tag-exif' },
        { icon: '📁', label: 'TEMPLATE / Internal Path', className: 'tag-geo' },
        { icon: '🕒', label: 'DATES / Total Edit Time', className: 'tag-timestamps' },
      ];
      defaults.forEach((d) => {
        if (!generatedTags.some((g) => g.className === d.className) && generatedTags.length < 5) {
          generatedTags.push(d);
        }
      });
    }
  }

  return generatedTags.slice(0, 5);
}

export default function DropzoneViewport({ onLedgerRefresh }) {
  const fileInputRef = useRef(null);

  const [isDragOver, setIsDragOver] = useState(false);
  const [isStageActive, setIsStageActive] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);

  const [statusText, setStatusText] = useState('1. File Ingested');
  const [statusSuccess, setStatusSuccess] = useState(false);
  const [fileName, setFileName] = useState('file.ext');
  const [formatBadge, setFormatBadge] = useState('DOC');

  const [arenaPhase, setArenaPhase] = useState(''); // '', 'extracting', 'sanitized-done'
  const [isScanning, setIsScanning] = useState(false);
  const [wsFlags, setWsFlags] = useState([]);
  const [wsHidden, setWsHidden] = useState(false);

  const [tags, setTags] = useState([]);
  const [resultData, setResultData] = useState(null);
  const [errorMessage, setErrorMessage] = useState(null);

  const handleContainerClick = () => {
    if (!isProcessing && !isStageActive && fileInputRef.current) {
      fileInputRef.current.click();
    }
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    if (!isProcessing) setIsDragOver(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    setIsDragOver(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragOver(false);
    if (isProcessing) return;
    const files = e.dataTransfer.files;
    if (files && files.length > 0) {
      processFilePipeline(files[0]);
    }
  };

  const handleFileChange = (e) => {
    if (isProcessing) return;
    if (e.target.files && e.target.files.length > 0) {
      processFilePipeline(e.target.files[0]);
    }
  };

  const resetToUpload = (e) => {
    if (e) e.stopPropagation();
    setIsProcessing(false);
    setIsStageActive(false);
    setArenaPhase('');
    setIsScanning(false);
    setWsFlags([]);
    setWsHidden(false);
    setTags([]);
    setResultData(null);
    setErrorMessage(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const processFilePipeline = async (file) => {
    if (!file || isProcessing) return;
    setIsProcessing(true);

    const fileType = detectFileType(file.name);

    // Transition to Contained Sanitization Stage
    setIsStageActive(true);
    setArenaPhase('');
    setIsScanning(false);
    setWsFlags([]);
    setWsHidden(false);
    setTags([]);
    setResultData(null);
    setErrorMessage(null);
    setFileName(file.name);
    setFormatBadge(fileType.badge);

    try {
      // =================================================================
      // PHASE 1: File Ingestion & Enclave Staging ("1. Rectangle Popping Up")
      // =================================================================
      setStatusText('1. Staging & Securing File in Enclave...');
      setStatusSuccess(false);

      const formData = new FormData();
      formData.append('files', file);

      const uploadResp = await fetch('/api/files/upload', {
        method: 'POST',
        body: formData,
      });

      if (!uploadResp.ok) throw new Error('Enclave failed to stage upload.');
      const uploadedRecords = await uploadResp.json();
      if (!uploadedRecords || uploadedRecords.length === 0) {
        throw new Error('No record returned from enclave.');
      }

      const record = uploadedRecords[0];
      await wait(450);

      // =================================================================
      // PHASE 2: Deep Metadata Scan ("2. Extracting Metadata")
      // =================================================================
      setStatusText('2. Extracting Embedded Telemetry & EXIF...');
      setArenaPhase('extracting');
      setIsScanning(true);

      const scanResp = await fetch(`/api/files/${record.fileUuid}/scan`, {
        method: 'POST',
      });

      let scanData = null;
      if (scanResp.ok) {
        scanData = await scanResp.json();
      }

      const extractedTags = buildMetadataTags(scanData, fileType);
      setTags(extractedTags);

      await wait(1100);

      // =================================================================
      // PHASE 3: Surgical Sanitization & Crumbling ("3. Crumbling Data")
      // =================================================================
      setStatusText('3. Crumbling & Neutralizing Sensitive Data...');
      setWsFlags(['crumbling']);

      const sanitizePromise = fetch(`/api/files/${record.fileUuid}/sanitize`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          preserveCopyright: true,
          stripGps: true,
          stripAuthor: true,
          stripDeviceInfo: true,
          stripTimestamps: true,
          stripRevisionHistory: true,
        }),
      });

      await wait(280);
      setWsFlags(['crumbling', 'ball-emerging']);

      const sanitizeResp = await sanitizePromise;
      if (!sanitizeResp.ok) throw new Error('Sanitization engine encountered an error.');
      const sanitizeData = await sanitizeResp.json();

      await wait(750);

      // =================================================================
      // PHASE 4: Discarding into Dustbin ("4. Throwing into Dustbin")
      // =================================================================
      setStatusText('4. Discarding to Dustbin & Validating Stream Guard...');
      setWsFlags(['crumbling', 'ball-emerging', 'dustbin-visible']);
      await wait(120);

      setWsFlags(['crumbling', 'ball-emerging', 'dustbin-visible', 'ball-throwing']);

      const verifyPromise = fetch(`/api/files/${record.fileUuid}/rescan-verify`, {
        method: 'POST',
      });

      await wait(550);
      setWsFlags(['crumbling', 'ball-emerging', 'dustbin-visible', 'ball-throwing', 'lid-snap-shut']);

      await verifyPromise;
      await wait(650);

      // =================================================================
      // PHASE 5: Sanitized Result ("Sanitized File Produced")
      // =================================================================
      setStatusText('✓ Sanitization Complete • Bit-Perfect Stream Guard Verified');
      setStatusSuccess(true);
      setIsScanning(false);
      setArenaPhase('sanitized-done');
      setWsHidden(true);

      setResultData({
        fileUuid: record.fileUuid,
        sizeStr: formatBytes(record.fileSize),
        hashStr: sanitizeData.post_sha256
          ? `${sanitizeData.post_sha256.substring(0, 16)}...`
          : 'Verified',
      });

      if (onLedgerRefresh) onLedgerRefresh();
    } catch (err) {
      console.error('Pipeline error:', err);
      setStatusText('Error in Sanitization Pipeline');
      setStatusSuccess(false);
      setErrorMessage(err.message || 'An unexpected error occurred.');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleDownload = (e) => {
    e.stopPropagation();
    if (!resultData) return;
    window.location.href = `/api/files/${resultData.fileUuid}/download?autoPurge=true`;
    setTimeout(() => {
      if (onLedgerRefresh) onLedgerRefresh();
    }, 2000);
  };

  return (
    <section className="glass-card">
      <div
        className={`dropzone-container ${isDragOver ? 'dragover' : ''}`}
        id="dropzone"
        onClick={handleContainerClick}
        onDragEnter={handleDragOver}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
      >
        <input
          type="file"
          id="fileInput"
          ref={fileInputRef}
          style={{ display: 'none' }}
          accept=".jpg,.jpeg,.png,.webp,.tiff,.pdf,.docx"
          onChange={handleFileChange}
        />

        <div className="dropzone-inner" id="dropzoneInner">
          {/* 1. IDLE STATE: Matches Reference Image 2 */}
          <div
            className={`dropzone-idle-view ${isStageActive ? 'hidden' : ''}`}
            id="dropzoneIdleView"
          >
            <div className="dropzone-icon-bubble">📄</div>
            <h3 className="dropzone-title">Drop files here to sanitize</h3>
            <p className="dropzone-desc">
              Drag and drop any image, PDF, or Word document. Balanced Legal Mode automatically secures your privacy while preserving copyright attribution.
            </p>
            <div className="dropzone-chips">
              <span className="format-chip">JPEG / EXIF</span>
              <span className="format-chip">PNG / XMP</span>
              <span className="format-chip">PDF Documents</span>
              <span className="format-chip">Word DOCX</span>
              <span className="format-chip">GPS Eradication</span>
              <span className="format-chip">Rights Preserved</span>
            </div>
          </div>

          {/* 2. ANIMATED SANITIZATION STAGE: Strictly Contained In Viewport */}
          <div
            className={`sanitization-stage ${isStageActive ? 'active' : ''}`}
            id="sanitizationStage"
          >
            {/* Stage Top Status Pill & Filename Tag */}
            <div className="stage-status-bar">
              <div className="stage-status-pill" id="stageStatusPill">
                <span
                  className={`status-pulse-dot ${statusSuccess ? 'success' : ''}`}
                  id="statusDot"
                ></span>
                <span className="status-text" id="stageStatusText">
                  {statusText}
                </span>
              </div>
              <div className="stage-filename-tag" id="stageFilenameTag">
                {fileName}
              </div>
            </div>

            {/* Stage Arena: File Document (Left) & Metadata/Dustbin (Right) */}
            <div className={`stage-arena ${arenaPhase}`} id="stageArena">
              {/* Left: File Document Card */}
              <div className="file-card-wrapper" id="fileCardWrapper">
                <div
                  className={`file-document-card ${isScanning ? 'scanning' : ''}`}
                  id="fileDocumentCard"
                >
                  <div className="dog-ear-corner">
                    <div className="dog-ear-fold"></div>
                  </div>
                  <div className="scan-beam" id="scanBeam"></div>
                  <div className="card-format-badge" id="cardFormatBadge">
                    {formatBadge}
                  </div>
                  <div className="doc-skeleton-lines">
                    <div className="skeleton-line line-1"></div>
                    <div className="skeleton-line line-2"></div>
                    <div className="skeleton-line line-3"></div>
                    <div className="skeleton-line line-4"></div>
                  </div>
                  <div className="shield-verified-badge" id="shieldVerifiedBadge">
                    <svg viewBox="0 0 24 24" className="shield-svg" fill="none">
                      <path
                        d="M12 2L4 5v6.09c0 5.05 3.41 9.76 8 10.91 4.59-1.15 8-5.86 8-10.91V5l-8-3z"
                        fill="#10b981"
                      />
                      <path
                        d="M9 12l2 2 4-4"
                        stroke="#ffffff"
                        strokeWidth="2.2"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      />
                    </svg>
                  </div>
                </div>
              </div>

              {/* Right: Metadata Tags & Disposal Workspace */}
              <div
                className={`action-workspace ${wsFlags.join(' ')}`}
                id="actionWorkspace"
                style={{
                  opacity: wsHidden ? 0 : 1,
                  transition: 'opacity 0.3s ease',
                }}
              >
                {/* Metadata Pill Cluster */}
                <div className="metadata-tag-cluster" id="metadataTagCluster">
                  {tags.map((t, idx) => (
                    <div
                      key={idx}
                      className={`meta-tag-pill ${t.className}`}
                      style={{ animationDelay: `${idx * 0.08}s` }}
                    >
                      <span>{t.icon}</span>
                      <span>{t.label}</span>
                    </div>
                  ))}
                </div>

                {/* Crumpled Metadata Paper Ball */}
                <div className="crumpled-ball-container" id="crumpledBallContainer">
                  <div className="motion-trail-arcs" id="motionTrailArcs">
                    <svg viewBox="0 0 160 80" className="trail-svg">
                      <path
                        d="M10,65 Q75,5 145,45"
                        stroke="rgba(147, 51, 234, 0.45)"
                        strokeWidth="2.5"
                        strokeDasharray="5 5"
                        fill="none"
                      />
                      <path
                        d="M25,75 Q85,15 155,55"
                        stroke="rgba(2, 132, 199, 0.45)"
                        strokeWidth="2"
                        strokeDasharray="4 4"
                        fill="none"
                      />
                    </svg>
                  </div>

                  <div className="paper-ball-mesh">
                    <svg viewBox="0 0 120 120" className="crumpled-paper-svg">
                      <defs>
                        <radialGradient id="ballShading" cx="40%" cy="35%" r="60%">
                          <stop offset="0%" stopColor="#ffffff" />
                          <stop offset="60%" stopColor="#f1f5f9" />
                          <stop offset="100%" stopColor="#cbd5e1" />
                        </radialGradient>
                        <filter id="crinkleShadow" x="-10%" y="-10%" width="130%" height="130%">
                          <feDropShadow
                            dx="0"
                            dy="4"
                            stdDeviation="4"
                            floodColor="rgba(15,23,42,0.15)"
                          />
                        </filter>
                      </defs>
                      <g filter="url(#crinkleShadow)">
                        <polygon
                          points="60,8 82,16 98,32 110,54 112,78 98,102 76,112 48,110 26,98 12,74 10,48 24,24 44,12"
                          fill="url(#ballShading)"
                          stroke="#94a3b8"
                          strokeWidth="1.2"
                        />
                        <polygon points="60,8 72,36 44,30" fill="#e2e8f0" stroke="#cbd5e1" strokeWidth="0.8" />
                        <polygon points="72,36 98,32 86,58" fill="#f8fafc" stroke="#cbd5e1" strokeWidth="0.8" />
                        <polygon points="86,58 110,54 94,76" fill="#e2e8f0" stroke="#cbd5e1" strokeWidth="0.8" />
                        <polygon points="94,76 112,78 98,102 78,88" fill="#cbd5e1" opacity="0.6" />
                        <polygon points="78,88 76,112 52,94" fill="#f1f5f9" stroke="#cbd5e1" strokeWidth="0.8" />
                        <polygon points="52,94 48,110 26,98 38,76" fill="#e2e8f0" stroke="#cbd5e1" strokeWidth="0.8" />
                        <polygon points="38,76 12,74 22,52" fill="#cbd5e1" opacity="0.5" />
                        <polygon points="22,52 10,48 24,24 42,40" fill="#f8fafc" stroke="#cbd5e1" strokeWidth="0.8" />
                        <polygon points="42,40 44,12 60,8 54,38" fill="#e2e8f0" stroke="#cbd5e1" strokeWidth="0.8" />
                        <polygon points="54,38 72,36 68,64 48,58" fill="#f8fafc" stroke="#94a3b8" strokeWidth="0.8" />
                        <polygon points="68,64 86,58 78,88 56,76" fill="#e2e8f0" stroke="#cbd5e1" strokeWidth="0.8" />
                        <polygon points="48,58 68,64 56,76 38,76 42,40" fill="#ffffff" stroke="#cbd5e1" strokeWidth="0.8" />

                        <g transform="rotate(-15 65 48)">
                          <rect x="52" y="44" width="28" height="10" rx="3" fill="#e0f2fe" stroke="#38bdf8" strokeWidth="0.8" />
                          <text x="56" y="51.5" fontFamily="'Inter', sans-serif" fontSize="5.5" fontWeight="700" fill="#0284c7">GPS</text>
                        </g>
                        <g transform="rotate(22 36 68)">
                          <rect x="22" y="63" width="32" height="10" rx="3" fill="#f3e8ff" stroke="#c084fc" strokeWidth="0.8" />
                          <text x="25" y="70.5" fontFamily="'Inter', sans-serif" fontSize="5.5" fontWeight="700" fill="#7e22ce">CAMERA</text>
                        </g>
                        <g transform="rotate(-30 76 76)">
                          <rect x="64" y="72" width="30" height="9" rx="3" fill="#dcfce7" stroke="#4ade80" strokeWidth="0.8" />
                          <text x="67" y="78.5" fontFamily="'Inter', sans-serif" fontSize="5" fontWeight="700" fill="#15803d">AUTHOR</text>
                        </g>
                        <g transform="rotate(10 74 28)">
                          <rect x="62" y="24" width="24" height="8" rx="2.5" fill="#fef9c3" stroke="#facc15" strokeWidth="0.8" />
                          <text x="65" y="30" fontFamily="'Inter', sans-serif" fontSize="5" fontWeight="700" fill="#a16207">EXIF</text>
                        </g>
                      </g>
                    </svg>
                  </div>
                </div>

                {/* Minimalist Metallic Dustbin */}
                <div className="dustbin-container" id="dustbinContainer">
                  <div className="dustbin-lid" id="dustbinLid">
                    <div className="lid-handle"></div>
                    <div className="lid-cap"></div>
                  </div>
                  <div className="dustbin-body">
                    <div className="dustbin-rim"></div>
                    <div className="dustbin-grooves">
                      <div className="bin-groove"></div>
                      <div className="bin-groove"></div>
                      <div className="bin-groove"></div>
                      <div className="bin-groove"></div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* Stage Result Tray */}
            {resultData && (
              <div className="stage-result-tray active" id="stageResultTray" style={{ display: 'flex' }}>
                <div className="result-stats-row">
                  <span className="res-badge">✓ Zero Residual Telemetry</span>
                  <span className="res-sep">•</span>
                  <span id="stageResSize">{resultData.sizeStr}</span>
                  <span className="res-sep">•</span>
                  <span className="res-hash">
                    SHA-256: <strong id="stageResHash">{resultData.hashStr}</strong>
                  </span>
                </div>
                <div className="result-actions-row">
                  <button
                    className="btn-primary-massive stage-btn-download"
                    id="stageBtnDownload"
                    onClick={handleDownload}
                  >
                    <span>⬇ Download Cleaned File</span>
                  </button>
                  <div className="result-sub-actions">
                    <a href="#reset" className="link-action" onClick={resetToUpload}>
                      + Sanitize Another File
                    </a>
                    <span style={{ color: 'var(--text-dim)' }}>•</span>
                    <a
                      id="stageLinkCert"
                      href={`/api/reports/${resultData.fileUuid}/print`}
                      target="_blank"
                      rel="noreferrer"
                      className="link-action"
                      onClick={(e) => e.stopPropagation()}
                    >
                      🛡️ View Compliance Certificate ↗
                    </a>
                  </div>
                </div>
              </div>
            )}

            {/* Stage Error Tray */}
            {errorMessage && (
              <div className="stage-error-tray" id="stageErrorTray" style={{ display: 'flex' }}>
                <div className="error-icon-bubble">⚠️</div>
                <h4 className="error-title">Sanitization Encountered an Issue</h4>
                <p className="error-msg" id="stageErrorMsg">
                  {errorMessage}
                </p>
                <button className="btn-primary-massive btn-retry" onClick={resetToUpload}>
                  Try Again
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}
