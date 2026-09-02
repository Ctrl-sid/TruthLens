import React from 'react';

export default function RationaleCard({ rationale, keyReasons, verdictBadgeColor, verdict, genuinenessScore, onOpenFeedback }) {
  const isNonClaim = genuinenessScore == null || 
    verdict === 'NON-VERIFIABLE IMAGE' || 
    verdict === 'NO CLAIM DETECTED' || 
    verdict === 'OCR UNRELIABLE' || 
    verdict?.includes('NON-VERIFIABLE') || 
    verdict?.includes('NO VERIFIABLE');

  return (
    <div className="h-100 d-flex flex-column justify-content-between">
      <div>
        <div className="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
          <h5 className="fw-bold text-white mb-0 d-flex align-items-center gap-2">
            <i className={`bi ${isNonClaim ? 'bi-question-circle text-warning' : 'bi-file-earmark-text text-cyan'}`}></i>
            <span>
              {isNonClaim ? "Why can't this image/input be verified?" : "Why is this News Genuine or Fake?"}
            </span>
          </h5>

          {onOpenFeedback && (
            <button
              type="button"
              className="btn btn-outline-secondary btn-sm rounded-pill text-cyan border-cyan border-opacity-30 d-flex align-items-center gap-1 small py-1 px-3"
              onClick={onOpenFeedback}
            >
              <i className="bi bi-chat-dots-fill"></i>
              <span>Rate Result / Contact Admin</span>
            </button>
          )}
        </div>

        <p className="text-light opacity-90 leading-relaxed mb-4">
          {rationale}
        </p>

        {keyReasons && keyReasons.length > 0 && (
          <div className="bg-dark bg-opacity-40 rounded-3 p-3 border border-secondary border-opacity-25 mb-3">
            <h6 className="small text-muted text-uppercase tracking-wider fw-bold mb-2">
              Key Verification Diagnostics
            </h6>
            <ul className="list-unstyled mb-0 d-flex flex-column gap-2">
              {keyReasons.map((reason, idx) => (
                <li key={idx} className="d-flex align-items-start gap-2 small text-light opacity-90">
                  <i className={`bi ${isNonClaim ? 'bi-info-circle-fill text-warning' : 'bi-check-circle-fill'}`} style={{ color: isNonClaim ? '#F59E0B' : (verdictBadgeColor || '#00f2fe') }}></i>
                  <span>{reason}</span>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </div>
  );
}
