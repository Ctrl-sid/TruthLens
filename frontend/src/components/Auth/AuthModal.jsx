import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';

export default function AuthModal({ show, onClose }) {
  const [isRegister, setIsRegister] = useState(false);
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [dpdpConsent, setDpdpConsent] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const { login, register } = useAuth();

  if (!show) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (isRegister && !dpdpConsent) {
      setError('Please acknowledge the DPDP Act 2023 data processing consent before creating an account.');
      return;
    }

    setLoading(true);

    try {
      if (isRegister) {
        await register(username, email, password, fullName);
      } else {
        await login(username, password);
      }
      onClose();
    } catch (err) {
      setError(typeof err === 'string' ? err : (err.message || 'Authentication failed. Please check credentials or code of conduct status.'));
    } finally {
      setLoading(false);
    }
  };

  const toggleMode = () => {
    setIsRegister(!isRegister);
    setError('');
  };

  return (
    <div className="modal fade show d-block" style={{ backgroundColor: 'rgba(0, 0, 0, 0.75)', backdropFilter: 'blur(8px)' }} tabIndex="-1">
      <div className="modal-dialog modal-dialog-centered">
        <div className="modal-content glass-card text-white border-secondary">
          <div className="modal-header border-secondary">
            <h5 className="modal-title fw-bold brand-font">
              {isRegister ? 'Create TruthLens Account' : 'Sign In to TruthLens'}
            </h5>
            <button type="button" className="btn-close btn-close-white" onClick={onClose}></button>
          </div>

          <form onSubmit={handleSubmit}>
            <div className="modal-body py-4">
              {error && (
                <div
                  className={`alert small mb-3 d-flex align-items-start gap-2.5 rounded-3 ${
                    error.toLowerCase().includes('banned') || error.toLowerCase().includes('suspended')
                      ? 'bg-danger bg-opacity-20 border border-danger text-light'
                      : 'bg-danger bg-opacity-10 border border-danger text-danger'
                  }`}
                >
                  <i
                    className={`bi fs-5 mt-0.5 ${
                      error.toLowerCase().includes('banned') || error.toLowerCase().includes('suspended')
                        ? 'bi-shield-x-fill text-danger'
                        : 'bi-exclamation-triangle-fill text-danger'
                    }`}
                  ></i>
                  <div>
                    <div className="fw-semibold">
                      {error.toLowerCase().includes('banned') || error.toLowerCase().includes('suspended')
                        ? 'Account Suspended'
                        : 'Authentication Notice'}
                    </div>
                    <div className="opacity-90">{error}</div>
                  </div>
                </div>
              )}

              {isRegister && (
                <div className="mb-3">
                  <label className="form-label small text-muted">Full Name</label>
                  <input
                    type="text"
                    className="form-control form-control-dark"
                    placeholder="e.g. Dr. Jane Smith"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                  />
                </div>
              )}

              <div className="mb-3">
                <label className="form-label small text-muted">Username</label>
                <input
                  type="text"
                  className="form-control form-control-dark"
                  placeholder="username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  required
                />
              </div>

              {isRegister && (
                <div className="mb-3">
                  <label className="form-label small text-muted">Email Address</label>
                  <input
                    type="email"
                    className="form-control form-control-dark"
                    placeholder="user@example.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                  />
                </div>
              )}

              <div className="mb-3">
                <label className="form-label small text-muted">Password</label>
                <input
                  type="password"
                  className="form-control form-control-dark"
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
              </div>

              {isRegister && (
                <div className="mb-3 form-check text-start">
                  <input
                    type="checkbox"
                    className="form-check-input"
                    id="dpdpConsentCheck"
                    checked={dpdpConsent}
                    onChange={(e) => setDpdpConsent(e.target.checked)}
                    required
                  />
                  <label className="form-check-label small text-muted" htmlFor="dpdpConsentCheck" style={{ fontSize: '11px', lineHeight: '1.4' }}>
                    I consent to TruthLens logging my verification queries and processing claim telemetry in compliance with the <strong>Digital Personal Data Protection (DPDP) Act 2023</strong>. I understand I can export or purge my verification history at any time.
                  </label>
                </div>
              )}
            </div>

            <div className="modal-footer border-secondary d-flex justify-content-between">
              <button
                type="button"
                className="btn btn-link text-cyan text-decoration-none p-0 small"
                onClick={toggleMode}
              >
                {isRegister ? 'Already have an account? Sign In' : 'Need an account? Register'}
              </button>

              <button type="submit" className="btn btn-cyan-gradient rounded-pill px-4 py-2" disabled={loading}>
                {loading ? 'Processing...' : (isRegister ? 'Create Account' : 'Sign In')}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
