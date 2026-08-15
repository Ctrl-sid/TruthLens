import React, { useEffect, useState } from 'react';
import { verifyService } from '../../services/verifyService';

export default function SourcesModal({ show, onClose }) {
  const [sources, setSources] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (show) {
      verifyService.getSources().then((data) => {
        setSources(data);
        setLoading(false);
      });
    }
  }, [show]);

  if (!show) return null;

  return (
    <div className="modal fade show d-block" style={{ backgroundColor: 'rgba(0, 0, 0, 0.75)', backdropFilter: 'blur(8px)' }} tabIndex="-1">
      <div className="modal-dialog modal-dialog-centered modal-lg">
        <div className="modal-content glass-card text-white border-secondary">
          <div className="modal-header border-secondary">
            <h5 className="modal-title fw-bold brand-font d-flex align-items-center gap-2">
              <i className="bi bi-shield-check text-cyan"></i>
              <span>TruthLens Reliable Wire & Fact-Checker Database</span>
            </h5>
            <button type="button" className="btn-close btn-close-white" onClick={onClose}></button>
          </div>

          <div className="modal-body py-4">
            <p className="text-muted small mb-4">
              TruthLens cross-references all submitted claims against accredited international news agencies, official fact-checking institutions, and scientific repositories.
            </p>

            {loading ? (
              <div className="text-center py-4">
                <span className="spinner-border text-cyan"></span>
              </div>
            ) : (
              <div className="table-responsive">
                <table className="table table-dark table-hover align-middle mb-0 border-secondary">
                  <thead>
                    <tr className="text-muted small text-uppercase">
                      <th>Source Name</th>
                      <th>Category</th>
                      <th>Credibility</th>
                      <th>Bias Index</th>
                      <th>Domain Link</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sources.map((src, idx) => (
                      <tr key={idx}>
                        <td className="fw-bold text-white">{src.name}</td>
                        <td><span className="badge bg-secondary bg-opacity-30 text-cyan">{src.category}</span></td>
                        <td>
                          <span className="fw-semibold text-success">{src.credibilityScore} / 100</span>
                        </td>
                        <td className="small text-muted">{src.biasRating}</td>
                        <td>
                          <a href={src.verifiedUrl} target="_blank" rel="noopener noreferrer" className="btn btn-outline-secondary btn-sm rounded-pill text-cyan border-cyan border-opacity-25 py-0 px-2 small">
                            Visit <i className="bi bi-box-arrow-up-right ms-1"></i>
                          </a>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
