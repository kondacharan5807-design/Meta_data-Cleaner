import React from 'react';

export default function AuditLedger({ files = [] }) {
  return (
    <section className="ledger-section">
      <div className="ledger-header">
        <h3>Session Activity &amp; Audit Ledger</h3>
        <span className="ledger-count" id="ledgerCount">
          {files.length === 0 ? '0 records' : `${files.length} record(s)`}
        </span>
      </div>

      <div className="table-glass">
        <table className="ledger-table">
          <thead>
            <tr>
              <th>File Name</th>
              <th>Date</th>
              <th>Status</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody id="ledgerTableBody">
            {files.length === 0 ? (
              <tr>
                <td
                  colSpan="4"
                  style={{
                    textAlign: 'center',
                    color: 'var(--text-dim)',
                    padding: '32px',
                  }}
                >
                  No files processed in this session yet. Drop a file above to begin.
                </td>
              </tr>
            ) : (
              files.map((f) => {
                const dateStr = f.uploadTimestamp
                  ? new Date(f.uploadTimestamp).toLocaleTimeString([], {
                      hour: '2-digit',
                      minute: '2-digit',
                      second: '2-digit',
                    })
                  : 'Just now';

                let statusBadge = <span className="status-badge status-staged">Staged</span>;
                if (f.status === 'SANITIZED' || f.status === 'VERIFIED') {
                  statusBadge = <span className="status-badge status-sanitized">✓ Sanitized</span>;
                } else if (f.status === 'EXPIRED_PURGED') {
                  statusBadge = <span className="status-badge status-staged">Purged</span>;
                }

                return (
                  <tr key={f.fileUuid || f.id}>
                    <td>
                      <div className="file-name-cell">
                        <div className="file-icon-mini">📄</div>
                        <span>{f.originalFilename}</span>
                      </div>
                    </td>
                    <td style={{ color: 'var(--text-muted)', fontSize: '13px' }}>{dateStr}</td>
                    <td>{statusBadge}</td>
                    <td>
                      {f.status !== 'EXPIRED_PURGED' ? (
                        <a
                          href={`/api/reports/${f.fileUuid}/print`}
                          target="_blank"
                          rel="noreferrer"
                          className="cert-link"
                        >
                          Certificate ↗
                        </a>
                      ) : (
                        <span style={{ color: 'var(--text-dim)', fontSize: '12px' }}>
                          GDPR Purged
                        </span>
                      )}
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}
