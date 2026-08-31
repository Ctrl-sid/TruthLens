import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';

export default function Footer({ onOpenAuth, onOpenHistory, onOpenSources, onOpenMessages, onOpenAdmin }) {
  const { user } = useAuth();
  const isAdmin = user && (user.role === 'ROLE_ADMIN' || user.username === 'admin');

  // Modal for footer informational views (About, Methodology, Ethics, Terms, Privacy, Submit Tip)
  const [modalContent, setModalContent] = useState(null);

  const openInfoModal = (title, icon, content) => {
    setModalContent({ title, icon, content });
  };

  const scrollToSection = (id) => {
    const el = document.getElementById(id);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' });
    }
  };

  return (
    <>
      <footer className="glass-nav pt-5 pb-4 mt-5 border-top border-secondary border-opacity-25" style={{ background: 'rgba(11, 15, 25, 0.95)' }}>
        <div className="container">
          {/* Main Top Grid */}
          <div className="row g-4 mb-5">
            {/* Brand Column */}
            <div className="col-lg-3 col-md-6">
              <div className="d-flex align-items-center gap-2 mb-3">
                <div className="position-relative d-flex align-items-center justify-content-center" style={{ width: 36, height: 36 }}>
                  <span className="position-absolute w-100 h-100 rounded-circle bg-cyan opacity-25 animate-ping"></span>
                  <i className="bi bi-eye-fill fs-4 text-cyan"></i>
                </div>
                <span className="fw-extrabold fs-4 tracking-wide text-white brand-font">
                  TRUTH<span className="text-cyan">LENS</span>
                </span>
              </div>

              <p className="small text-muted mb-4 leading-relaxed" style={{ fontSize: '0.875rem' }}>
                TruthLens is an AI-powered verification platform designed to combat misinformation through NLP entity extraction, TF-IDF vector similarity, and real-time wire cross-referencing.
              </p>

              {/* Social Media Links inspired by Snopes */}
              <div className="d-flex align-items-center gap-2">
                <a href="https://twitter.com" target="_blank" rel="noopener noreferrer" className="btn btn-outline-secondary btn-sm rounded-circle p-2 d-flex align-items-center justify-content-center text-light opacity-75 hover-cyan" style={{ width: 34, height: 34 }}>
                  <i className="bi bi-twitter-x"></i>
                </a>
                <a href="https://facebook.com" target="_blank" rel="noopener noreferrer" className="btn btn-outline-secondary btn-sm rounded-circle p-2 d-flex align-items-center justify-content-center text-light opacity-75 hover-cyan" style={{ width: 34, height: 34 }}>
                  <i className="bi bi-facebook"></i>
                </a>
                <a href="https://linkedin.com" target="_blank" rel="noopener noreferrer" className="btn btn-outline-secondary btn-sm rounded-circle p-2 d-flex align-items-center justify-content-center text-light opacity-75 hover-cyan" style={{ width: 34, height: 34 }}>
                  <i className="bi bi-linkedin"></i>
                </a>
                <a href="https://github.com/Ctrl-sid/TruthLens" target="_blank" rel="noopener noreferrer" className="btn btn-outline-secondary btn-sm rounded-circle p-2 d-flex align-items-center justify-content-center text-light opacity-75 hover-cyan" style={{ width: 34, height: 34 }}>
                  <i className="bi bi-github"></i>
                </a>
                <a href="https://mastodon.social" target="_blank" rel="noopener noreferrer" className="btn btn-outline-secondary btn-sm rounded-circle p-2 d-flex align-items-center justify-content-center text-light opacity-75 hover-cyan" style={{ width: 34, height: 34 }}>
                  <i className="bi bi-mastodon"></i>
                </a>
              </div>
            </div>

            {/* Column 1: Company */}
            <div className="col-lg-2 col-md-6 col-6">
              <h6 className="fw-bold text-white mb-3 text-uppercase tracking-wider" style={{ fontSize: '0.85rem' }}>
                Company
              </h6>
              <ul className="list-unstyled d-flex flex-column gap-2 mb-0" style={{ fontSize: '0.875rem' }}>
                <li>
                  <button
                    className="btn btn-link text-muted p-0 text-decoration-none text-start hover-cyan small"
                    onClick={() => openInfoModal(
                      'About TruthLens',
                      'bi-info-circle',
                      'TruthLens is an automated fact-checking and media genuineness verification platform. Built with React.js, Spring Boot, and custom NLP vector engines, TruthLens provides transparent, objective ratings (0-100%) by cross-referencing trusted news agencies (Reuters, AP, Snopes, PolitiFact, WHO, NASA).'
                    )}
                  >
                    About Us
                  </button>
                </li>
                <li>
                  <button
                    className="btn btn-link text-muted p-0 text-decoration-none text-start hover-cyan small"
                    onClick={() => openInfoModal(
                      'Verification Methodology',
                      'bi-cpu',
                      'TruthLens employs a multi-tiered pipeline: (1) Named Entity Recognition (NER), (2) Clickbait & Sensationalism Classification, (3) TF-IDF + Cosine Similarity cross-referencing against verified wire reports and debunked hoax databases, (4) Forensic Image Noise & EXIF analysis, and (5) Real-time external knowledge integration.'
                    )}
                  >
                    Methodology
                  </button>
                </li>
                <li>
                  <button className="btn btn-link text-muted p-0 text-decoration-none text-start hover-cyan small" onClick={onOpenSources}>
                    Accredited Sources
                  </button>
                </li>
                <li>
                  <button
                    className="btn btn-link text-muted p-0 text-decoration-none text-start hover-cyan small"
                    onClick={() => openInfoModal(
                      'Ethics & Standards',
                      'bi-shield-check',
                      'TruthLens adheres to strict neutrality and non-partisanship principles in line with the International Fact-Checking Network (IFCN) guidelines. We do not accept political contributions, and all algorithms operate on transparent linguistic and semantic evidentiary criteria.'
                    )}
                  >
                    Ethics & Standards
                  </button>
                </li>
                <li>
                  <button
                    className="btn btn-link text-muted p-0 text-decoration-none text-start hover-cyan small"
                    onClick={() => {
                      if (user) onOpenMessages();
                      else onOpenAuth();
                    }}
                  >
                    Submit a Claim / Tip
                  </button>
                </li>
              </ul>
            </div>

            {/* Column 2: Navigate */}
            <div className="col-lg-2 col-md-6 col-6">
              <h6 className="fw-bold text-white mb-3 text-uppercase tracking-wider" style={{ fontSize: '0.85rem' }}>
                Navigate
              </h6>
              <ul className="list-unstyled d-flex flex-column gap-2 mb-0" style={{ fontSize: '0.875rem' }}>
                <li>
                  <button className="btn btn-link text-muted p-0 text-decoration-none text-start hover-cyan small" onClick={() => scrollToSection('analyzer')}>
                    Verification Workspace
                  </button>
                </li>
                <li>
                  <button className="btn btn-link text-muted p-0 text-decoration-none text-start hover-cyan small" onClick={onOpenHistory}>
                    Verification History
                  </button>
                </li>
                <li>
                  <button className="btn btn-link text-muted p-0 text-decoration-none text-start hover-cyan small" onClick={onOpenSources}>
                    Wire Repositories
                  </button>
                </li>
                <li>
                  <button
                    className="btn btn-link text-muted p-0 text-decoration-none text-start hover-cyan small"
                    onClick={() => {
                      scrollToSection('analyzer');
                    }}
                  >
                    Claim Search
                  </button>
                </li>
              </ul>
            </div>

            {/* Column 3: Sections */}
            <div className="col-lg-2 col-md-6 col-6">
              <h6 className="fw-bold text-white mb-3 text-uppercase tracking-wider" style={{ fontSize: '0.85rem' }}>
                Sections
              </h6>
              <ul className="list-unstyled d-flex flex-column gap-2 mb-0" style={{ fontSize: '0.875rem' }}>
                <li>
                  <button
                    className="btn btn-link text-muted p-0 text-decoration-none text-start hover-cyan small"
                    onClick={() => openInfoModal('Science & Space Verification', 'bi-stars', 'TruthLens cross-references astronomical and scientific announcements directly with accredited mission agencies including NASA, ESA, ISRO, and peer-reviewed journals.')}
                  >
                    Science & Space
                  </button>
                </li>
                <li>
                  <button
                    className="btn btn-link text-muted p-0 text-decoration-none text-start hover-cyan small"
                    onClick={() => openInfoModal('Public Health Verification', 'bi-heart-pulse', 'Health advisories and epidemic claims are verified against the World Health Organization (WHO), CDC, Ministry of Health registries, and accredited medical databases.')}
                  >
                    Public Health
                  </button>
                </li>
                <li>
                  <button
                    className="btn btn-link text-muted p-0 text-decoration-none text-start hover-cyan small"
                    onClick={() => openInfoModal('Deepfakes & AI Forensics', 'bi-cpu', 'Multimedia claims are inspected using Error Level Analysis (ELA), visual compression matrices, and audio/video forensic heuristics to detect synthesized media.')}
                  >
                    Deepfakes & AI
                  </button>
                </li>
                <li>
                  <button
                    className="btn btn-link text-muted p-0 text-decoration-none text-start hover-cyan small"
                    onClick={() => openInfoModal('Health Misinformation', 'bi-shield-exclamation', 'Viral remedies and unproven miracle cures are cross-referenced with medical fact-checking repositories (Snopes, PolitiFact, BoomLive).')}
                  >
                    Health Misinformation
                  </button>
                </li>
                <li>
                  <span className="badge bg-secondary bg-opacity-25 text-cyan border border-cyan border-opacity-25 px-2 py-1 small">
                    FactBot AI v2.4
                  </span>
                </li>
              </ul>
            </div>

            {/* Column 4: Account & Gateway */}
            <div className="col-lg-3 col-md-6 col-6">
              <h6 className="fw-bold text-white mb-3 text-uppercase tracking-wider" style={{ fontSize: '0.85rem' }}>
                Account & Gateway
              </h6>
              <ul className="list-unstyled d-flex flex-column gap-2 mb-0" style={{ fontSize: '0.875rem' }}>
                {user ? (
                  <>
                    <li className="d-flex align-items-center gap-2">
                      <i className="bi bi-person-check-fill text-cyan"></i>
                      <span className="text-light small">{user.fullName || user.username}</span>
                    </li>
                    <li>
                      <button className="btn btn-link text-muted p-0 text-decoration-none text-start hover-cyan small" onClick={onOpenHistory}>
                        My Claim History
                      </button>
                    </li>
                    <li>
                      <button className="btn btn-link text-muted p-0 text-decoration-none text-start hover-cyan small" onClick={onOpenMessages}>
                        User-Admin Inbox
                      </button>
                    </li>
                    {isAdmin && (
                      <li>
                        <button className="btn btn-link text-warning p-0 text-decoration-none text-start fw-bold small d-flex align-items-center gap-1" onClick={onOpenAdmin}>
                          <i className="bi bi-shield-lock-fill"></i> Command Center
                        </button>
                      </li>
                    )}
                  </>
                ) : (
                  <>
                    <li>
                      <button className="btn btn-link text-muted p-0 text-decoration-none text-start hover-cyan small" onClick={onOpenAuth}>
                        Sign In / Register
                      </button>
                    </li>
                    <li>
                      <button className="btn btn-link text-muted p-0 text-decoration-none text-start hover-cyan small" onClick={onOpenAuth}>
                        Create Free Account
                      </button>
                    </li>
                  </>
                )}
                <li>
                  <button
                    className="btn btn-link text-muted p-0 text-decoration-none text-start hover-cyan small"
                    onClick={() => openInfoModal(
                      'API & Developer Access',
                      'bi-code-slash',
                      'TruthLens REST API endpoints (/api/verify/claim, /api/sources, /api/nlp/analyze) allow developers to integrate real-time claim verification and NLP diagnostics into news apps, social feeds, and browser extensions.'
                    )}
                  >
                    API Documentation
                  </button>
                </li>
              </ul>
            </div>
          </div>

          {/* Snopes-inspired Copyright & Legal Disclosures */}
          <div className="pt-4 border-top border-secondary border-opacity-25">
            <div className="row align-items-center gy-3">
              <div className="col-md-7">
                <p className="small text-muted mb-1" style={{ fontSize: '0.8rem' }}>
                  © 1995 - 2026 by TruthLens AI Systems, Inc. This material may not be reproduced without permission.
                </p>
                <p className="small text-muted mb-0" style={{ fontSize: '0.75rem' }}>
                  TruthLens and the TruthLens logo are registered trademarks and service marks of TruthLens Platform.
                </p>
              </div>

              {/* Legal Links */}
              <div className="col-md-5 text-md-end">
                <div className="d-flex flex-wrap justify-content-md-end gap-3" style={{ fontSize: '0.78rem' }}>
                  <button
                    className="btn btn-link text-muted p-0 text-decoration-none hover-cyan"
                    onClick={() => openInfoModal(
                      'Terms & Conditions',
                      'bi-journal-text',
                      'By accessing TruthLens, you agree to utilize our automated analysis results for informational, educational, and verification purposes. TruthLens does not warrant 100% infallibility and encourages cross-referencing primary documentary sources.'
                    )}
                  >
                    Terms & Conditions
                  </button>
                  <span className="text-secondary opacity-50">•</span>
                  <button
                    className="btn btn-link text-muted p-0 text-decoration-none hover-cyan"
                    onClick={() => openInfoModal(
                      'Privacy Policy',
                      'bi-shield-lock',
                      'TruthLens respects your privacy. Submitted claims, URLs, and images processed via our NLP engine are not sold to third parties. User accounts and verification history are secured with bcrypt password encryption and JWT bearer authentication.'
                    )}
                  >
                    Privacy Policy
                  </button>
                  <span className="text-secondary opacity-50">•</span>
                  <button
                    className="btn btn-link text-muted p-0 text-decoration-none hover-cyan"
                    onClick={() => openInfoModal(
                      'DMCA Policy',
                      'bi-file-earmark-lock',
                      'TruthLens complies with the Digital Millennium Copyright Act (DMCA). Content extracts and headlines analyzed for fact-checking fall under fair use educational and transformative reporting standards.'
                    )}
                  >
                    DMCA Policy
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </footer>

      {/* Informational Glass Modal */}
      {modalContent && (
        <div className="modal fade show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.75)', zIndex: 1060 }} tabIndex="-1">
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content glass-card border-secondary text-white p-2" style={{ background: 'rgba(15, 23, 42, 0.95)' }}>
              <div className="modal-header border-secondary border-opacity-25 pb-3">
                <h5 className="modal-title fw-bold d-flex align-items-center gap-2 text-cyan">
                  <i className={`bi ${modalContent.icon}`}></i>
                  <span>{modalContent.title}</span>
                </h5>
                <button type="button" className="btn-close btn-close-white" onClick={() => setModalContent(null)}></button>
              </div>
              <div className="modal-body py-4">
                <p className="text-light opacity-90 leading-relaxed mb-0" style={{ fontSize: '0.95rem' }}>
                  {modalContent.content}
                </p>
              </div>
              <div className="modal-footer border-secondary border-opacity-25 pt-2">
                <button type="button" className="btn btn-cyan-gradient rounded-pill px-4 py-1.5" onClick={() => setModalContent(null)}>
                  Close
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
