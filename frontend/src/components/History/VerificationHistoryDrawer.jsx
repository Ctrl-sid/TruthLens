import React from 'react';

export default function VerificationHistoryDrawer({ show, onClose, history, currentUser, onSelectHistoryItem, onDeleteHistoryItem, onClearAllHistory }) {
  if (!show) return null;

  return (
    <div className="offcanvas offcanvas-end show glass-card text-white border-secondary" tabIndex="-1" style={{ visibility: 'visible', width: '420px', zIndex: 1055 }}>
      <div className="offcanvas-header border-bottom border-secondary d-flex justify-content-between align-items-center">
        <h5 className="offcanvas-title fw-bold brand-font d-flex align-items-center gap-2 mb-0">
          <i className="bi bi-clock-history text-cyan"></i>
          <span>Personal Search History</span>
        </h5>
        <button type="button" className="btn-close btn-close-white" onClick={onClose}></button>
      </div>

      <div className="offcanvas-body">
        {currentUser ? (
          <>
            <div className="d-flex justify-content-between align-items-center mb-3 bg-dark bg-opacity-40 p-2.5 rounded border border-secondary border-opacity-25">
              <span className="small text-muted">
                Private history for <strong className="text-cyan font-monospace">@{currentUser.username}</strong>
              </span>
              {history && history.length > 0 && (
                <button
                  type="button"
                  className="btn btn-outline-danger btn-sm py-0.5 px-2 text-danger border-danger border-opacity-30 rounded-pill small"
                  onClick={() => {
                    if (window.confirm('Clear all your saved search history?')) {
                      onClearAllHistory();
                    }
                  }}
                >
                  Clear All
                </button>
              )}
            </div>

            {history && history.length > 0 ? (
              <div className="d-flex flex-column gap-3">
                {history.map((item, idx) => (
                  <div
                    key={item.id || idx}
                    className="bg-dark bg-opacity-40 p-3 rounded-3 border border-secondary border-opacity-25 position-relative group hover-border-cyan"
                  >
                    <div className="d-flex justify-content-between align-items-center mb-1">
                      <span className="badge bg-secondary bg-opacity-30 text-cyan small">{item.inputType}</span>
                      <div className="d-flex align-items-center gap-2">
                        <span
                          className="badge px-2 py-0.5 small rounded-pill"
                          style={{ backgroundColor: `${item.verdictBadgeColor}22`, color: item.verdictBadgeColor, border: `1px solid ${item.verdictBadgeColor}` }}
                        >
                          {item.genuinenessScore}% - {item.verdict}
                        </span>
                        <button
                          type="button"
                          className="btn btn-link text-muted hover-danger p-0 border-0"
                          title="Delete this history item"
                          onClick={(e) => {
                            e.stopPropagation();
                            onDeleteHistoryItem(item.id);
                          }}
                        >
                          <i className="bi bi-trash text-muted opacity-75 hover-text-danger"></i>
                        </button>
                      </div>
                    </div>

                    <p
                      className="small text-white mb-2 fw-semibold line-clamp-2 cursor-pointer"
                      onClick={() => {
                        onSelectHistoryItem(item);
                        onClose();
                      }}
                    >
                      {item.claimSummary}
                    </p>

                    <span className="small text-muted d-block" style={{ fontSize: '0.75rem' }}>
                      {item.timestamp}
                    </span>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-5 text-muted">
                <i className="bi bi-inbox fs-1 mb-2 d-block opacity-50"></i>
                <p className="mb-0">Your history is empty.</p>
                <span className="small">Searches you make while signed in will be saved privately here.</span>
              </div>
            )}
          </>
        ) : (
          <div className="text-center py-5 text-muted">
            <i className="bi bi-shield-lock fs-1 text-cyan mb-3 d-block"></i>
            <h6 className="fw-bold text-white mb-2">Sign In Required for History Privacy</h6>
            <p className="small text-muted mb-0">
              To protect your privacy, search prompts are kept confidential and only saved to registered user accounts.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
