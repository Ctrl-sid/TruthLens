import React from 'react';

export default function TruthScoreGauge({ score, verdict, badgeColor }) {
  const radius = 70;
  const strokeWidth = 12;
  const normalizedRadius = radius - strokeWidth * 0.5;
  const circumference = normalizedRadius * 2 * Math.PI;
  const strokeDashoffset = circumference - (score / 100) * circumference;

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
            {score}<span className="fs-6 text-muted">%</span>
          </span>
          <span className="small text-muted text-uppercase tracking-wider font-monospace">Genuineness</span>
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
