import React from 'react';

export default function Header() {
  return (
    <header className="header-glass">
      <div className="brand-wrapper">
        <div className="brand-icon">🛡️</div>
        <div className="brand-text">
          <h1>Meta<span>Shield</span></h1>
          <p>Intelligent Privacy &amp; Metadata Sanitizer</p>
        </div>
      </div>

      {/* Academic Ownership Attribution */}
      <div className="academic-badge">
        <span className="dot"></span>
        <span><strong>Malla Reddy University</strong></span>
        <span>•</span>
        <span>
          Architects: <strong>Charan Konda (2511CS040113)</strong> &amp; <strong>J. Naga Phani (2511CS040132)</strong>
        </span>
        <span>•</span>
        <span style={{ fontFamily: 'var(--font-mono)', color: 'var(--accent-violet)' }}>CS BETA – 17</span>
      </div>
    </header>
  );
}
