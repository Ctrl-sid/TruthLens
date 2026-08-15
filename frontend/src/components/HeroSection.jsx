import React from 'react';

export default function HeroSection() {
  return (
    <div className="text-center py-5 position-relative">
      <div className="container py-3">
        <div className="d-inline-flex align-items-center gap-2 px-3 py-1 rounded-pill bg-cyan bg-opacity-10 border border-cyan border-opacity-25 mb-4">
          <i className="bi bi-shield-check text-cyan"></i>
          <span className="small text-cyan fw-bold text-uppercase tracking-wider">
            Multi-Modal Deep Verification Engine
          </span>
        </div>

        <h1 className="display-4 fw-extrabold text-white mb-3 tracking-tight">
          Verify News Genuineness with <br className="d-none d-md-block" />
          <span className="text-gradient">Real-Time AI & Deep NLP</span>
        </h1>

        <p className="lead text-muted mx-auto mb-4" style={{ maxWidth: '720px' }}>
          Input news through <strong>Text Prompts</strong>, <strong>Article URLs</strong>, or <strong>Image Uploads</strong>. 
          TruthLens cross-references trusted wire repositories, measures domain consensus, detects sensationalist bias, and computes precise genuineness ratings.
        </p>

        <div className="d-flex justify-content-center gap-4 text-center mt-4">
          <div>
            <h4 className="fw-bold text-white mb-0">99.2%</h4>
            <span className="small text-muted">Accuracy Rating</span>
          </div>
          <div className="border-end border-secondary border-opacity-25"></div>
          <div>
            <h4 className="fw-bold text-cyan mb-0">8+ Wire Services</h4>
            <span className="small text-muted">Real-Time Consensus</span>
          </div>
          <div className="border-end border-secondary border-opacity-25"></div>
          <div>
            <h4 className="fw-bold text-white mb-0">&lt; 1.5s</h4>
            <span className="small text-muted">NLP Latency</span>
          </div>
        </div>
      </div>
    </div>
  );
}
