import React from 'react';

export default function TruthScoreGauge({ score, verdict, badgeColor }) {
  const radius = 70;
  const strokeWidth = 12;
  const normalizedRadius = radius - strokeWidth * 0.5;
  const circumference = normalizedRadius * 2 * Math.PI;
  const displayScore = score != null ? score : 50;
  const strokeDashoffset = circumference - (displayScore / 100) * circumference;

  const isNonVerifiable = score == null || 
    verdict === 'NON-VERIFIABLE INPUT' || 
    verdict === 'NO VERIFIABLE CLAIM' || 
    verdict === 'OCR INSUFFICIENT' || 
    verdict?.includes('NON-VERIFIABLE') || 
    verdict?.includes('INSUFFICIENT');

  if (isNonVerifiable && score == null) {
    const isOcrInsufficient = verdict === 'OCR INSUFFICIENT';
    const isNoClaim = verdict === 'NO VERIFIABLE CLAIM';
    const color = badgeColor || (isOcrInsufficient ? '#D97706' : '#94A3B8');

    return (
      <div className="text-center p-3 d-flex flex-column align-items-center justify-content-center h-100">
        <div
          className="rounded-circle d-flex align-items-center justify-content-center mb-3 shadow-lg"
          style={{
            width: 110,
            height: 110,
            background: `${color}15`,
            border: `2px dashed ${color}`
          }}
        >
          <i className={`bi ${isOcrInsufficient ? 'bi-file-earmark-x-fill' : 'bi-question-diamond-fill'} fs-1`} style={{ color }}></i>
        </div>

        <span
          className="badge px-3 py-1.5 fs-6 rounded-pill fw-bold text-uppercase mb-2"
          style={{ backgroundColor: `${color}25`, color, border: `1px solid ${color}` }}
        >
          <i className="bi bi-info-circle me-1"></i> {verdict || 'Non-Verifiable Input'}
        </span>
        <p className="small text-muted mb-0" style={{ fontSize: '0.825rem' }}>
          {isNoClaim ? 
            "Image lacks a declarative factual assertion. Genuineness Score: N/A." :
           (isOcrInsufficient ? 
            "OCR quality insufficient for automated verification. Score: N/A." : 
            "No declarative factual assertion detected. Score: N/A.")}
        </p>
      </div>
    );
  }

  return (
    <div className="text-center p-3">
      <div className="position-relative d-inline-block">
        <svg height={radius * 2} width={radius * 2} className="transform -rotate-90">
          <circle
            className="gauge-background"
            r={normalizedRadius}
            cx={radius}
            cy={radius}
          />
          <circle
            className="gauge-progress"
            stroke={badgeColor || '#00f2fe'}
            strokeDasharray={circumference + ' ' + circumference}
            style={{ strokeDashoffset }}
            r={normalizedRadius}
            cx={radius}
            cy={radius}
          />
        </svg>

        <div className="position-absolute top-50 start-50 translate-middle text-center">
          <span className="display-6 fw-extrabold text-white d-block lh-1">
            {displayScore}<span className="fs-6 text-muted">/100</span>
          </span>
          <span className="small text-muted text-uppercase tracking-wider font-monospace" style={{ fontSize: '0.65rem' }}>
            Support Score
          </span>
        </div>
      </div>

      <div className="mt-3">
        <span
          className="badge px-3 py-2 fs-6 rounded-pill fw-bold text-uppercase"
          style={{ backgroundColor: `${badgeColor}22`, color: badgeColor, border: `1px solid ${badgeColor}` }}
        >
          <i className="bi bi-shield-fill-check me-2"></i>
          {verdict}
        </span>
      </div>
    </div>
  );
}
