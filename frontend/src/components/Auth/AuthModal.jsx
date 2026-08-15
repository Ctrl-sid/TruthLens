import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';

export default function AuthModal({ show, onClose }) {
  const [isRegister, setIsRegister] = useState(false);
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const { login, register } = useAuth();

  if (!show) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
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
                <div className="alert alert-danger bg-danger bg-opacity-10 border-danger text-danger small mb-3">
                  {error}
                </div>
              )}

              {isRegister && (
                <div className="mb-3">
                  <label className="form-label small text-muted">Full Name</label>
                  <input
                    type="text"
                    className="form-control form-control-dark"
                    placeholder="Jane Doe"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    required
                  />
                </div>
              )}

              <div className="mb-3">
                <label className="form-label small text-muted">Username</label>
                <input
                  type="text"
                  className="form-control form-control-dark"
                  placeholder="janedoe"
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
                    placeholder="jane@example.com"
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
            </div>

            <div className="modal-footer border-secondary d-flex justify-content-between">
              <button
                type="button"
                className="btn btn-link text-cyan text-decoration-none p-0 small"
                onClick={() => setIsRegister(!isRegister)}
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
