import React from 'react';

export default function RationaleCard({ 
  rationale, 
  keyReasons, 
  verdictBadgeColor, 
  verdict, 
  genuinenessScore, 
  explicitClaimText,
  inferredContext,
  visualContextDescription,
  claimDisambiguationOptions = [],
  baseSupportScore,
  contradictionPenalty,
  supportScore,
  suggestedAction,
  stopReason,
  pipelineStatus,
  onDisambiguate,
  onOpenFeedback 
}) {
  const isAmbiguousSocialPost = verdict?.includes('AMBIGUOUS') || verdict?.includes('AMBIGUOUS SOCIAL POST');
  const isNonClaim = genuinenessScore == null || 
    verdict === 'NON-VERIFIABLE IMAGE' || 
    verdict === 'NO CLAIM DETECTED' || 
    verdict === 'OCR UNRELIABLE' || 
    verdict?.includes('NON-VERIFIABLE') || 
    verdict?.includes('NO VERIFIABLE') ||
    isAmbiguousSocialPost;

  const getDynamicHeader = () => {
    if (isAmbiguousSocialPost) {
      return "Social Post: Ambiguous Context & Target Disambiguation";
    }
    if (verdict?.includes('INSUFFICIENT')) {
      return "Why is there insufficient evidence?";
    }
    if (verdict === 'OCR UNRELIABLE' || verdict === 'OCR_UNRELIABLE' || verdict === 'NON-VERIFIABLE IMAGE') {
      return "Why couldn't the claim be extracted from this image?";
    }
    if (verdict?.includes('CONTRADICTED') || verdict?.includes('HOAX') || verdict?.includes('FABRICATED')) {
      return "Why is this claim contradicted?";
    }
    if (verdict?.includes('PARTIALLY')) {
      return "Why is this claim only partially supported?";
    }
    if (verdict?.includes('SUPPORTED') || verdict?.includes('GENUINE')) {
      return "Why is this claim supported?";
    }
    if (isNonClaim) {
      return "Why can't this input be verified?";
    }
    return "Why is this claim supported or contradicted?";
  };

  return (
    <div className="h-100 d-flex flex-column justify-content-between">
      <div>
        <div className="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
          <h5 className="fw-bold text-white mb-0 d-flex align-items-center gap-2">
            <i className={`bi ${isAmbiguousSocialPost ? 'bi-shield-exclamation text-warning' : (isNonClaim ? 'bi-question-circle text-warning' : 'bi-file-earmark-text text-cyan')}`}></i>
            <span>
              {getDynamicHeader()}
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

        {/* Verification Stop Reason if Blocked */}
        {stopReason && isNonClaim && (
          <div className="bg-rose-950 bg-opacity-30 rounded-3 p-3 border border-rose-500 border-opacity-30 mb-3">
            <div className="small fw-bold text-rose-300 mb-1 d-flex align-items-center gap-1.5">
              <i className="bi bi-shield-slash-fill text-rose-400"></i>
              <span>Verification Stop Reason</span>
            </div>
            <p className="text-slate-300 text-xs mb-0 font-monospace">
              {stopReason}
            </p>
          </div>
        )}

        {/* Suggested Next Action */}
        {suggestedAction && (
          <div className="bg-sky-950 bg-opacity-40 rounded-3 p-3 border border-cyan border-opacity-30 mb-3">
            <div className="small fw-bold text-cyan mb-1 d-flex align-items-center gap-1.5">
              <i className="bi bi-lightbulb-fill text-warning"></i>
              <span>Suggested Next Step</span>
            </div>
            <p className="text-light opacity-90 text-xs mb-0">
              {suggestedAction}
            </p>
          </div>
        )}

        {/* Explicit vs Inferred vs Visual Context Card */}
        {(explicitClaimText || inferredContext || visualContextDescription) && (
          <div className="bg-slate-900 bg-opacity-70 rounded-3 p-3 border border-secondary border-opacity-25 mb-3">
            <div className="small fw-bold text-uppercase text-cyan mb-2 d-flex align-items-center gap-1">
              <i className="bi bi-layers-half"></i>
              <span>Context & Proposition Separation</span>
            </div>
            
            <div className="d-flex flex-column gap-2 small">
              {explicitClaimText && (
                <div className="d-flex align-items-start gap-2">
                  <span className="badge bg-secondary bg-opacity-40 text-light border border-secondary border-opacity-50 text-uppercase shrink-0 font-monospace" style={{ fontSize: '10px' }}>
                    Explicit Post Text
                  </span>
                  <span className="text-light opacity-90 fst-italic">"{explicitClaimText}"</span>
                </div>
              )}

              {inferredContext && (
                <div className="d-flex align-items-start gap-2">
                  <span className="badge bg-sky-900 bg-opacity-40 text-cyan border border-cyan border-opacity-30 text-uppercase shrink-0 font-monospace" style={{ fontSize: '10px' }}>
                    Inferred Context
                  </span>
                  <span className="text-light opacity-90">{inferredContext}</span>
                </div>
              )}

              {visualContextDescription && (
                <div className="d-flex align-items-start gap-2">
                  <span className="badge bg-purple-900 bg-opacity-40 text-purple-300 border border-purple-500 border-opacity-30 text-uppercase shrink-0 font-monospace" style={{ fontSize: '10px' }}>
                    Visual Context
                  </span>
                  <span className="text-light opacity-90">{visualContextDescription}</span>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Interactive Claim Disambiguation Options */}
        {claimDisambiguationOptions && claimDisambiguationOptions.length > 0 && (
          <div className="bg-dark bg-opacity-50 rounded-3 p-3 border border-warning border-opacity-30 mb-3">
            <div className="small fw-bold text-warning mb-1 d-flex align-items-center gap-1.5">
              <i className="bi bi-cursor-fill"></i>
              <span>Select Exact Verification Target:</span>
            </div>
            <p className="text-slate-400 text-xs mb-2.5">
              Because this social media post does not contain a declarative news claim, choose what you would like to independently verify:
            </p>
            <div className="d-flex flex-column gap-2">
              {claimDisambiguationOptions.map((opt, idx) => (
                <button
                  key={idx}
                  type="button"
                  className="btn btn-sm btn-outline-info text-start d-flex align-items-center justify-content-between p-2 rounded-2 border-opacity-40 hover-lift text-xs text-light"
                  style={{ background: 'rgba(14, 165, 233, 0.08)' }}
                  onClick={() => onDisambiguate && onDisambiguate(opt)}
                >
                  <span className="d-flex align-items-center gap-2">
                    <span className="badge bg-info bg-opacity-20 text-info font-monospace">{idx + 1}</span>
                    <span>{opt}</span>
                  </span>
                  <i className="bi bi-arrow-right-circle text-info ms-2 shrink-0"></i>
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Normalized Score Breakdown if applicable */}
        {baseSupportScore != null && contradictionPenalty != null && (
          <div className="d-flex flex-wrap gap-2 mb-3">
            <span className="badge bg-dark bg-opacity-60 text-slate-300 border border-slate-700 py-1.5 px-2.5 font-monospace text-xs d-flex align-items-center gap-1.5">
              <i className="bi bi-calculator text-cyan"></i>
              <span>Base Support: <strong>{baseSupportScore}/100</strong></span>
            </span>
            {contradictionPenalty > 0 && (
              <span className="badge bg-rose-950 bg-opacity-50 text-rose-300 border border-rose-500 border-opacity-40 py-1.5 px-2.5 font-monospace text-xs d-flex align-items-center gap-1.5">
                <i className="bi bi-dash-circle text-rose-400"></i>
                <span>Contradiction Penalty: <strong>-{contradictionPenalty} pts</strong></span>
              </span>
            )}
            <span className="badge bg-emerald-950 bg-opacity-40 text-emerald-300 border border-emerald-500 border-opacity-40 py-1.5 px-2.5 font-monospace text-xs d-flex align-items-center gap-1.5">
              <i className="bi bi-check2-circle text-emerald-400"></i>
              <span>Final Support Score: <strong>{supportScore != null ? `${supportScore}/100` : 'N/A'}</strong></span>
            </span>
          </div>
        )}

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
