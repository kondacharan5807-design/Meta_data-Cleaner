import React, { useState, useEffect, useCallback } from 'react';
import Header from './components/Header.jsx';
import Hero from './components/Hero.jsx';
import DropzoneViewport from './components/DropzoneViewport.jsx';
import AuditLedger from './components/AuditLedger.jsx';
import Footer from './components/Footer.jsx';

export default function App() {
  const [ledgerFiles, setLedgerFiles] = useState([]);

  const loadAuditLedger = useCallback(async () => {
    try {
      const resp = await fetch('/api/files');
      if (!resp.ok) return;
      const files = await resp.json();
      setLedgerFiles(Array.isArray(files) ? files : []);
    } catch (e) {
      console.error('Failed to load audit ledger', e);
    }
  }, []);

  useEffect(() => {
    loadAuditLedger();
  }, [loadAuditLedger]);

  return (
    <>
      {/* Atmospheric Pastel Gradient Background Orbs */}
      <div className="ambient-background">
        <div className="gradient-orb orb-cerulean"></div>
        <div className="gradient-orb orb-violet"></div>
        <div className="gradient-orb orb-mint"></div>
      </div>

      {/* Frosted Glass Navigation Bar */}
      <Header />

      {/* Main Content Layout */}
      <main className="main-wrapper">
        {/* Hero Title & Subtitle */}
        <Hero />

        {/* Main Glassmorphic Container Card & Contained Upload Animation */}
        <DropzoneViewport onLedgerRefresh={loadAuditLedger} />

        {/* Simple & Airy Audit Ledger Table */}
        <AuditLedger files={ledgerFiles} />
      </main>

      {/* Clean Academic Footer */}
      <Footer />
    </>
  );
}
