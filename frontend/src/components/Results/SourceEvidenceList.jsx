import React from 'react';

export default function SourceEvidenceList({ sources }) {
  if (!sources || sources.length === 0) return null;

  return (
    <div className="p-3">
      <h6 className="small text-muted text-uppercase fw-bold mb-3 d-flex align-items-center gap-2">
        <i className="bi bi-diagram-3 text-cyan"></i>
        <span>Reliable Source Consensus & Cross-References</span>
      </h6>

      <div className="d-flex flex-column gap-2">
        {sources.map((src, idx) => (
          <div key={idx} className="bg-dark bg-opacity-40 p-3 rounded-3 border border-secondary border-opacity-25 d-flex flex-column flex-sm-row justify-content-between align-items-start align-items-sm-center gap-2">
            <div>
              <div className="d-flex align-items-center gap-2 mb-1">
                <span className="fw-bold text-white">{src.sourceName}</span>
                <span className="badge bg-primary bg-opacity-25 text-cyan border border-cyan border-opacity-25 px-2 py-0.5">
                  Authority: {src.credibilityRating}/100
                </span>
              </div>
              <a href={src.url} target="_blank" rel="noopener noreferrer" className="small text-muted text-decoration-none hover-cyan d-flex align-items-center gap-1">
                <i className="bi bi-box-arrow-up-right"></i>
                <span>{src.domain}</span>
              </a>
            </div>

            <div className="text-sm-end">
              <span
                className={`badge px-2 py-1 small rounded-pill fw-bold ${
                  src.verdictBySource?.includes('True') || src.verdictBySource?.includes('Confirmed') || src.verdictBySource?.includes('Reference') || src.verdictBySource?.includes('Consensus')
                    ? 'bg-success bg-opacity-20 text-success border border-success border-opacity-25'
                    : src.verdictBySource?.includes('Unconfirmed') || src.verdictBySource?.includes('No Wire')
                    ? 'bg-warning bg-opacity-20 text-warning border border-warning border-opacity-25'
                    : 'bg-danger bg-opacity-20 text-danger border border-danger border-opacity-25'
                }`}
              >
                {src.verdictBySource}
              </span>
              <span className="d-block small text-muted mt-1">
                {src.matchPercentage}% Claim Match
              </span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
