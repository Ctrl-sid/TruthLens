import React, { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import HeroSection from './components/HeroSection';
import TextInput from './components/InputTabs/TextInput';
import UrlInput from './components/InputTabs/UrlInput';
import ImageInput from './components/InputTabs/ImageInput';
import TruthScoreGauge from './components/Results/TruthScoreGauge';
import RationaleCard from './components/Results/RationaleCard';
import NlpAnalysisCard from './components/Results/NlpAnalysisCard';
import SourceEvidenceList from './components/Results/SourceEvidenceList';
import ImageHeatmap from './components/Results/ImageHeatmap';
import ClaimFeedbackModal from './components/Results/ClaimFeedbackModal';
import UserMessagingDrawer from './components/User/UserMessagingDrawer';
import AdminDashboard from './components/Admin/AdminDashboard';
import SampleNewsCarousel from './components/Presets/SampleNewsCarousel';
import AuthModal from './components/Auth/AuthModal';
import VerificationHistoryDrawer from './components/History/VerificationHistoryDrawer';
import SourcesModal from './components/Sources/SourcesModal';
import { verifyService } from './services/verifyService';
import { AuthProvider, useAuth } from './context/AuthContext';
import confetti from 'canvas-confetti';

function AppContent() {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState('TEXT'); // TEXT, URL, IMAGE
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [activeResultTab, setActiveResultTab] = useState('nlp'); // nlp, sources, image
  
  // Modals & Drawers
  const [showAuthModal, setShowAuthModal] = useState(false);
  const [showHistoryDrawer, setShowHistoryDrawer] = useState(false);
  const [showSourcesModal, setShowSourcesModal] = useState(false);
  const [showFeedbackModal, setShowFeedbackModal] = useState(false);
  const [showMessagingDrawer, setShowMessagingDrawer] = useState(false);
  const [showAdminDashboard, setShowAdminDashboard] = useState(false);

  const [history, setHistory] = useState([]);

  useEffect(() => {
    // Load isolated local user history
    if (user && user.username) {
      verifyService.getHistory(user.username).then((data) => setHistory(data));
    } else {
      setHistory([]);
    }
  }, [user]);

  const handleVerify = async (type, content, title = '') => {
    setLoading(true);
    setResult(null);

    // Simulate multi-step scanner timing for realistic UX
    setTimeout(async () => {
      const res = await verifyService.verifyClaim(type, content, title);
      setResult(res);
      setLoading(false);

      // Save to isolated user history if user is signed in
      if (user && user.username) {
        const updatedHistory = verifyService.saveHistoryItem(user.username, res);
        setHistory(updatedHistory);
      }

      // Trigger celebratory confetti if claim is verified genuine!
      if (res.genuinenessScore >= 85) {
        confetti({
          particleCount: 60,
          spread: 70,
          origin: { y: 0.6 }
        });
      }
    }, 1200);
  };

  const handleSelectPreset = (type, content) => {
    setActiveTab(type);
    handleVerify(type, content);
  };

  const handleSelectHistoryItem = (item) => {
    setResult(item);
    window.scrollTo({ top: document.getElementById('results-section')?.offsetTop - 80, behavior: 'smooth' });
  };

  const handleDeleteHistoryItem = (itemId) => {
    if (user && user.username) {
      const updated = verifyService.deleteHistoryItem(user.username, itemId);
      setHistory(updated);
    }
  };

  const handleClearAllHistory = () => {
    if (user && user.username) {
      const updated = verifyService.clearAllHistory(user.username);
      setHistory(updated);
    }
  };

  return (
    <div className="min-vh-100 d-flex flex-column bg-dark-slate">
      <Navbar
        onOpenAuth={() => setShowAuthModal(true)}
        onOpenHistory={() => setShowHistoryDrawer(true)}
        onOpenSources={() => setShowSourcesModal(true)}
        onOpenMessages={() => {
          if (!user) setShowAuthModal(true);
          else setShowMessagingDrawer(true);
        }}
        onOpenAdmin={() => setShowAdminDashboard(true)}
      />

      <main className="flex-grow-1">
        <HeroSection />

        {/* Input & Verification Workspace */}
        <section id="analyzer" className="container mb-5">
          <div className="glass-card p-4 p-md-5">
            <h4 className="fw-bold text-white mb-4 d-flex align-items-center gap-2 brand-font">
              <i className="bi bi-cpu text-cyan"></i>
              <span>News Input Workspace</span>
            </h4>

            {/* Input Mode Tabs */}
            <ul className="nav nav-pills mb-4 gap-2 border-bottom border-secondary border-opacity-25 pb-3">
              <li className="nav-item">
                <button
                  className={`nav-link d-flex align-items-center gap-2 ${activeTab === 'TEXT' ? 'active' : ''}`}
                  onClick={() => setActiveTab('TEXT')}
                >
                  <i className="bi bi-chat-left-text"></i> Text / Prompt
                </button>
              </li>
              <li className="nav-item">
                <button
                  className={`nav-link d-flex align-items-center gap-2 ${activeTab === 'URL' ? 'active' : ''}`}
                  onClick={() => setActiveTab('URL')}
                >
                  <i className="bi bi-link-45deg fs-5"></i> Article URL
                </button>
              </li>
              <li className="nav-item">
                <button
                  className={`nav-link d-flex align-items-center gap-2 ${activeTab === 'IMAGE' ? 'active' : ''}`}
                  onClick={() => setActiveTab('IMAGE')}
                >
                  <i className="bi bi-image"></i> Image Upload
                </button>
              </li>
            </ul>

            {/* Input Form Views */}
            <div className="mb-4">
              {activeTab === 'TEXT' && <TextInput onVerify={handleVerify} loading={loading} />}
              {activeTab === 'URL' && <UrlInput onVerify={handleVerify} loading={loading} />}
              {activeTab === 'IMAGE' && <ImageInput onVerify={handleVerify} loading={loading} />}
            </div>

            {/* Scanner Loading State */}
            {loading && (
              <div className="text-center py-5">
                <div className="scanner-container mb-3">
                  <div className="scanner-ring"></div>
                </div>
                <h5 className="fw-bold text-white mb-1">Scanning Claim across Wire Repositories...</h5>
                <p className="text-muted small mb-0">Executing NER Entity Extraction, Subjectivity Classifier, and Source Cross-Referencing</p>
              </div>
            )}

            {/* Verification Results Dashboard */}
            {result && !loading && (
              <div id="results-section" className="mt-5 pt-4 border-top border-secondary border-opacity-25">
                <div className="d-flex justify-content-between align-items-center mb-4">
                  <div>
                    <span className="badge bg-secondary bg-opacity-30 text-cyan mb-1">Verification Report #{result.id}</span>
                    <h4 className="fw-bold text-white mb-0">{result.claimSummary}</h4>
                  </div>
                  <span className="small text-muted">{result.timestamp}</span>
                </div>

                <div className="row g-4 mb-4">
                  {/* Gauge Meter */}
                  <div className="col-lg-4">
                    <div className="glass-card p-4 text-center h-100 d-flex flex-column justify-content-center" style={{ background: 'rgba(15, 23, 42, 0.6)' }}>
                      <TruthScoreGauge
                        score={result.genuinenessScore}
                        verdict={result.verdict}
                        badgeColor={result.verdictBadgeColor}
                      />
                    </div>
                  </div>

                  {/* Rationale Card */}
                  <div className="col-lg-8">
                    <div className="glass-card p-4 h-100" style={{ background: 'rgba(15, 23, 42, 0.6)' }}>
                      <RationaleCard
                        rationale={result.rationale}
                        keyReasons={result.keyReasons}
                        verdictBadgeColor={result.verdictBadgeColor}
                        onOpenFeedback={() => {
                          if (!user) setShowAuthModal(true);
                          else setShowFeedbackModal(true);
                        }}
                      />
                    </div>
                  </div>
                </div>

                {/* Deep Analysis Tabs */}
                <div className="glass-card p-4" style={{ background: 'rgba(15, 23, 42, 0.6)' }}>
                  <ul className="nav nav-tabs border-secondary border-opacity-25 mb-3 gap-2">
                    <li className="nav-item">
                      <button
                        className={`nav-link text-light opacity-75 border-0 ${activeResultTab === 'nlp' ? 'active text-cyan fw-bold border-bottom border-cyan' : ''}`}
                        onClick={() => setActiveResultTab('nlp')}
                      >
                        <i className="bi bi-cpu me-1"></i> NLP & Linguistic Diagnostics
                      </button>
                    </li>
                    <li className="nav-item">
                      <button
                        className={`nav-link text-light opacity-75 border-0 ${activeResultTab === 'sources' ? 'active text-cyan fw-bold border-bottom border-cyan' : ''}`}
                        onClick={() => setActiveResultTab('sources')}
                      >
                        <i className="bi bi-diagram-3 me-1"></i> Source Consensus ({result.sources?.length || 0})
                      </button>
                    </li>
                    {result.imageAnalysis && (
                      <li className="nav-item">
                        <button
                          className={`nav-link text-light opacity-75 border-0 ${activeResultTab === 'image' ? 'active text-cyan fw-bold border-bottom border-cyan' : ''}`}
                          onClick={() => setActiveResultTab('image')}
                        >
                          <i className="bi bi-image me-1"></i> Image Forensic Heatmap
                        </button>
                      </li>
                    )}
                  </ul>

                  {activeResultTab === 'nlp' && <NlpAnalysisCard nlpData={result.nlpAnalysis} />}
                  {activeResultTab === 'sources' && <SourceEvidenceList sources={result.sources} />}
                  {activeResultTab === 'image' && <ImageHeatmap imageAnalysis={result.imageAnalysis} />}
                </div>
              </div>
            )}
          </div>

          {/* Interactive Presets Section */}
          <SampleNewsCarousel onSelectPreset={handleSelectPreset} />
        </section>
      </main>

      <footer className="glass-nav py-4 mt-auto border-top border-secondary border-opacity-25">
        <div className="container d-flex flex-column flex-md-row justify-content-between align-items-center gap-3">
          <div className="d-flex align-items-center gap-2">
            <i className="bi bi-eye-fill text-cyan fs-5"></i>
            <span className="fw-bold text-white brand-font">TruthLens</span>
            <span className="small text-muted">— AI News Genuineness Verification Platform</span>
          </div>

          <span className="small text-muted">
            Built with React, Spring Boot, Spring Security (JWT), PostgreSQL & Admin Gateway
          </span>
        </div>
      </footer>

      {/* Modals & Drawers */}
      <AuthModal show={showAuthModal} onClose={() => setShowAuthModal(false)} />
      <VerificationHistoryDrawer
        show={showHistoryDrawer}
        onClose={() => setShowHistoryDrawer(false)}
        history={history}
        currentUser={user}
        onSelectHistoryItem={handleSelectHistoryItem}
        onDeleteHistoryItem={handleDeleteHistoryItem}
        onClearAllHistory={handleClearAllHistory}
      />
      <SourcesModal show={showSourcesModal} onClose={() => setShowSourcesModal(false)} />

      {/* MESSAGING, FEEDBACK & ADMIN MODALS */}
      <ClaimFeedbackModal
        show={showFeedbackModal}
        onClose={() => setShowFeedbackModal(false)}
        claimResult={result}
      />
      <UserMessagingDrawer
        show={showMessagingDrawer}
        onClose={() => setShowMessagingDrawer(false)}
        currentUser={user}
      />
      <AdminDashboard
        show={showAdminDashboard}
        onClose={() => setShowAdminDashboard(false)}
        onLaunchAnalyzer={() => document.getElementById('analyzer')?.scrollIntoView({ behavior: 'smooth' })}
      />
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}
