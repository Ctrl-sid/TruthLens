import React from 'react';

export default function NlpAnalysisCard({ nlpData }) {
  if (!nlpData) return null;

  const {
    extractedEntities,
    sentimentScore,
    subjectivityScore,
    clickbaitRating,
    toneAnalysis,
    readabilityScore,
    exaggerationFlags
  } = nlpData;

  return (
    <div className="p-3">
      <div className="row g-3 mb-4">
        {/* Entities Extracted */}
        <div className="col-md-6">
          <div className="bg-dark bg-opacity-40 p-3 rounded-3 border border-secondary border-opacity-25">
            <h6 className="small text-muted text-uppercase fw-bold mb-2 d-flex align-items-center gap-2">
              <i className="bi bi-tags text-cyan"></i>
              <span>Extracted Named Entities (NER)</span>
            </h6>
            <div className="d-flex flex-wrap gap-1">
              {extractedEntities && extractedEntities.length > 0 ? (
                extractedEntities.map((ent, idx) => (
                  <span key={idx} className="badge bg-secondary bg-opacity-30 text-cyan border border-cyan border-opacity-25 px-2 py-1">
                    {ent}
                  </span>
                ))
              ) : (
                <span className="text-muted small">No specific entities detected</span>
              )}
            </div>
          </div>
        </div>

        {/* Tone & Readability */}
        <div className="col-md-6">
          <div className="bg-dark bg-opacity-40 p-3 rounded-3 border border-secondary border-opacity-25">
            <h6 className="small text-muted text-uppercase fw-bold mb-2 d-flex align-items-center gap-2">
              <i className="bi bi-chat-quote text-cyan"></i>
              <span>Linguistic Tone & Readability</span>
            </h6>
            <div className="d-flex justify-content-between align-items-center mb-1">
              <span className="small text-light">Tone classification:</span>
              <span className="fw-semibold text-cyan small">{toneAnalysis}</span>
            </div>
            <div className="d-flex justify-content-between align-items-center">
              <span className="small text-light">Flesch Readability:</span>
              <span className="fw-semibold text-white small">{readabilityScore} / 100</span>
            </div>
          </div>
        </div>
      </div>

      {/* Metrics Sliders */}
      <div className="row g-3">
        <div className="col-md-4">
          <div className="bg-dark bg-opacity-40 p-3 rounded-3 border border-secondary border-opacity-25 text-center">
            <span className="small text-muted d-block mb-1">Subjectivity Index</span>
            <h5 className="fw-bold text-white mb-2">{Math.round((subjectivityScore || 0) * 100)}%</h5>
            <div className="progress bg-secondary bg-opacity-30" style={{ height: 6 }}>
              <div
                className={`progress-bar ${subjectivityScore > 0.6 ? 'bg-danger' : 'bg-success'}`}
                style={{ width: `${(subjectivityScore || 0) * 100}%` }}
              ></div>
            </div>
            <span className="small text-muted mt-1 d-block" style={{ fontSize: '0.75rem' }}>
              {subjectivityScore > 0.6 ? 'Highly Subjective' : 'Objective Phrasing'}
            </span>
          </div>
        </div>

        <div className="col-md-4">
          <div className="bg-dark bg-opacity-40 p-3 rounded-3 border border-secondary border-opacity-25 text-center">
            <span className="small text-muted d-block mb-1">Clickbait / Sensationalism</span>
            <h5 className="fw-bold text-white mb-2">{clickbaitRating || 0}%</h5>
            <div className="progress bg-secondary bg-opacity-30" style={{ height: 6 }}>
              <div
                className={`progress-bar ${clickbaitRating > 50 ? 'bg-danger' : 'bg-info'}`}
                style={{ width: `${clickbaitRating || 0}%` }}
              ></div>
            </div>
            <span className="small text-muted mt-1 d-block" style={{ fontSize: '0.75rem' }}>
              {clickbaitRating > 50 ? 'Exaggerated Headline' : 'Low Clickbait Risk'}
            </span>
          </div>
        </div>

        <div className="col-md-4">
          <div className="bg-dark bg-opacity-40 p-3 rounded-3 border border-secondary border-opacity-25 text-center">
            <span className="small text-muted d-block mb-1">Sentiment Polarity</span>
            <h5 className="fw-bold text-white mb-2">{(sentimentScore || 0) > 0 ? `+${sentimentScore}` : sentimentScore}</h5>
            <div className="progress bg-secondary bg-opacity-30" style={{ height: 6 }}>
              <div
                className="progress-bar bg-cyan"
                style={{ width: `${((sentimentScore || 0) + 1) * 50}%` }}
              ></div>
            </div>
            <span className="small text-muted mt-1 d-block" style={{ fontSize: '0.75rem' }}>
              -1.0 (Negative) to +1.0 (Positive)
            </span>
          </div>
        </div>
      </div>

      {exaggerationFlags && exaggerationFlags.length > 0 && (
        <div className="alert alert-warning bg-warning bg-opacity-10 border-warning border-opacity-25 mt-3 mb-0 text-warning small">
          <i className="bi bi-exclamation-triangle-fill me-2"></i>
          <strong>Linguistic Flags:</strong> {exaggerationFlags.join(' • ')}
        </div>
      )}
    </div>
  );
}
