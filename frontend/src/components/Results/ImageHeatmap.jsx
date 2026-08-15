import React from 'react';

export default function ImageHeatmap({ imageAnalysis }) {
  if (!imageAnalysis) return null;

  const { detectedHeadlineText, manipulationProbability, exifStatus, anomalyFlags, heatmapOverlayUrl } = imageAnalysis;

  return (
    <div className="p-3">
      <div className="row g-3">
        <div className="col-md-5">
          <div className="bg-dark bg-opacity-40 p-2 rounded-3 border border-secondary border-opacity-25 text-center">
            <img src={heatmapOverlayUrl} alt="Analysis Preview" className="img-fluid rounded mb-2" style={{ maxHeight: '180px', objectFit: 'cover' }} />
            <span className="small text-muted d-block">Digital Noise Overlay & Forensic Filter</span>
          </div>
        </div>

        <div className="col-md-7">
          <h6 className="fw-bold text-white mb-2">OCR Text Extracted</h6>
          <p className="small text-cyan bg-dark bg-opacity-50 p-2 rounded border border-cyan border-opacity-25 mb-3 font-monospace">
            "{detectedHeadlineText}"
          </p>

          <div className="d-flex justify-content-between align-items-center mb-2">
            <span className="small text-muted">Manipulation Risk:</span>
            <span className={`fw-bold small ${manipulationProbability > 50 ? 'text-danger' : 'text-success'}`}>
              {manipulationProbability}%
            </span>
          </div>

          <div className="d-flex justify-content-between align-items-center mb-3">
            <span className="small text-muted">EXIF Metadata:</span>
            <span className="badge bg-secondary bg-opacity-30 text-light">{exifStatus}</span>
          </div>

          <h6 className="small text-muted text-uppercase fw-bold mb-1">Forensic Artifact Flags</h6>
          <ul className="list-unstyled mb-0">
            {anomalyFlags && anomalyFlags.map((flag, idx) => (
              <li key={idx} className="small text-light opacity-90 d-flex align-items-center gap-2 mb-1">
                <i className="bi bi-circle-fill text-warning" style={{ fontSize: '0.4rem' }}></i>
                <span>{flag}</span>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  );
}
