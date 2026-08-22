import React from 'react';

export default function ClaimOriginCard({ originDiscovery, userClaim }) {
  if (!originDiscovery) return null;

  const {
    originalPublisher,
    originalDomain,
    originalHeadline,
    originalUrl,
    publishedDate,
    provenanceType,
    distortionAnalysis,
    crossReferencedConsensus,
    originMatchConfidence
  } = originDiscovery;

  const getProvenanceBadge = (type) => {
    switch (type) {
      case 'AUTHENTIC_REPRODUCTION':
        return {
          label: 'Authentic News Reproduction',
          colorClass: 'bg-success bg-opacity-20 text-success border-success',
          icon: 'bi-patch-check-fill'
        };
      case 'ALTERED_DISTORTION':
        return {
          label: 'Altered / Distorted Headline',
          colorClass: 'bg-danger bg-opacity-20 text-danger border-danger',
          icon: 'bi-exclamation-octagon-fill'
        };
      case 'DOCUMENTED_HOAX':
        return {
          label: 'Documented Viral Hoax',
          colorClass: 'bg-danger bg-opacity-25 text-danger border-danger',
          icon: 'bi-shield-x'
        };
      case 'UNVERIFIED_ORIGIN':
      default:
        return {
          label: 'Unverified Online Claim',
          colorClass: 'bg-warning bg-opacity-20 text-warning border-warning',
          icon: 'bi-question-diamond-fill'
        };
    }
  };

  const badgeInfo = getProvenanceBadge(provenanceType);

  return (
    <div className="p-3">
      {/* Header Banner */}
      <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3">
        <h6 className="small text-muted text-uppercase fw-bold m-0 d-flex align-items-center gap-2">
          <i className="bi bi-compass text-cyan"></i>
          <span>Claim Origin & Provenance Discovery</span>
        </h6>

        <span className={`badge px-3 py-1.5 rounded-pill fw-bold border border-opacity-50 d-inline-flex align-items-center gap-1.5 ${badgeInfo.colorClass}`}>
          <i className={`bi ${badgeInfo.icon}`}></i>
          <span>{badgeInfo.label}</span>
        </span>
      </div>

      {/* Identified Origin Box */}
      <div className="bg-dark bg-opacity-40 p-3.5 rounded-3 border border-secondary border-opacity-25 mb-3">
        <div className="d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center gap-2 mb-2">
          <div className="d-flex align-items-center gap-2">
            <div className="avatar-icon bg-cyan bg-opacity-10 text-cyan p-2 rounded-circle border border-cyan border-opacity-25">
              <i className="bi bi-building"></i>
            </div>
            <div>
              <span className="small text-muted d-block text-uppercase fw-semibold" style={{ fontSize: '0.72rem' }}>
                Primary Originating Publisher
              </span>
              <span className="fw-bold text-white fs-6">
                {originalPublisher || 'Unverified Web'}
              </span>
            </div>
          </div>

          <div className="d-flex align-items-center gap-2">
            {publishedDate && (
              <span className="badge bg-secondary bg-opacity-25 text-light border border-secondary border-opacity-25 small">
                <i className="bi bi-clock-history me-1"></i>
                {publishedDate}
              </span>
            )}
            {originMatchConfidence > 0 && (
              <span className="badge bg-cyan bg-opacity-15 text-cyan border border-cyan border-opacity-30 small">
                Match: {originMatchConfidence}%
              </span>
            )}
          </div>
        </div>

        {/* Original Published Headline */}
        {originalHeadline && (
          <div className="mt-2.5 p-2.5 rounded-2 bg-black bg-opacity-30 border border-secondary border-opacity-15">
            <span className="small text-muted d-block mb-1" style={{ fontSize: '0.75rem' }}>
              <i className="bi bi-newspaper me-1"></i> Original Published Headline / Record:
            </span>
            <div className="fw-semibold text-light fst-italic">
              "{originalHeadline}"
            </div>
            {originalUrl && (
              <a
                href={originalUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="small text-cyan text-decoration-none mt-2 d-inline-flex align-items-center gap-1 hover-underline"
              >
                <span>View original report at {originalDomain || 'source'}</span>
                <i className="bi bi-box-arrow-up-right" style={{ fontSize: '0.75rem' }}></i>
              </a>
            )}
          </div>
        )}
      </div>

      {/* Semantic Comparison & Distortion Analysis */}
      {distortionAnalysis && (
        <div className="bg-dark bg-opacity-30 p-3 rounded-3 border border-secondary border-opacity-20 mb-3">
          <div className="d-flex align-items-center gap-2 mb-1.5 text-cyan">
            <i className="bi bi-shield-check"></i>
            <span className="fw-bold small text-uppercase" style={{ fontSize: '0.75rem', letterSpacing: '0.5px' }}>
              Provenance & Distortion Analysis
            </span>
          </div>
          <p className="text-light small m-0 opacity-90 leading-relaxed">
            {distortionAnalysis}
          </p>
        </div>
      )}

      {/* Cross-Referenced Wire Consensus Summary */}
      {crossReferencedConsensus && (
        <div className="p-2.5 rounded-3 bg-cyan bg-opacity-5 border border-cyan border-opacity-20 d-flex align-items-center gap-2">
          <i className="bi bi-diagram-3 text-cyan fs-6"></i>
          <div className="small text-light">
            <span className="fw-semibold text-cyan">Consensus: </span>
            <span>{crossReferencedConsensus}</span>
          </div>
        </div>
      )}
    </div>
  );
}
