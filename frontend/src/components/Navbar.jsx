import React from 'react';
import { useAuth } from '../context/AuthContext';

export default function Navbar({ onOpenAuth, onOpenHistory, onOpenSources, onOpenMessages, onOpenAdmin }) {
  const { user, logout } = useAuth();
  const isAdmin = user && (user.role === 'ROLE_ADMIN' || user.username === 'admin');

  return (
    <nav className="navbar navbar-expand-lg sticky-top glass-nav py-3">
      <div className="container">
        <a className="navbar-brand d-flex align-items-center gap-2" href="#">
          <div className="position-relative d-flex align-items-center justify-content-center" style={{ width: 40, height: 40 }}>
            <span className="position-absolute w-100 h-100 rounded-circle bg-cyan opacity-25 animate-ping"></span>
            <i className="bi bi-eye-fill fs-3 text-cyan"></i>
          </div>
          <span className="fw-extrabold fs-4 tracking-wide text-white brand-font">
            TRUTH<span className="text-cyan">LENS</span>
          </span>
          <span className="badge bg-primary bg-opacity-25 text-cyan border border-cyan border-opacity-25 ms-1 small">
            AI v2.4 NLP
          </span>
        </a>

        <button className="navbar-toggler border-0 text-white" type="button" data-bs-toggle="collapse" data-bs-target="#navContent">
          <i className="bi bi-list fs-2"></i>
        </button>

        <div className="collapse navbar-collapse" id="navContent">
          <ul className="navbar-nav me-auto ms-lg-4 mb-2 mb-lg-0 gap-lg-2">
            <li className="nav-item">
              <a className="nav-link text-light opacity-75 active fw-semibold" href="#analyzer">Analyzer</a>
            </li>
            <li className="nav-item">
              <a className="nav-link text-light opacity-75 fw-semibold" href="#presets">Presets</a>
            </li>
            <li className="nav-item">
              <button className="btn nav-link text-light opacity-75 fw-semibold border-0" onClick={onOpenSources}>
                Reliable Sources
              </button>
            </li>
          </ul>

          <div className="d-flex align-items-center gap-2">
            <button className="btn btn-outline-light btn-sm rounded-pill px-3 py-2 border-opacity-25 d-flex align-items-center gap-2" onClick={onOpenHistory}>
              <i className="bi bi-clock-history text-cyan"></i>
              <span>History</span>
            </button>

            {user && (
              <button className="btn btn-outline-info btn-sm rounded-pill px-3 py-2 border-opacity-25 d-flex align-items-center gap-2 text-cyan" onClick={onOpenMessages}>
                <i className="bi bi-chat-left-text-fill"></i>
                <span>Messaging Gateway</span>
              </button>
            )}

            {isAdmin && (
              <button className="btn btn-danger btn-sm rounded-pill px-3 py-2 d-flex align-items-center gap-2 fw-bold border border-warning border-opacity-40" onClick={onOpenAdmin}>
                <i className="bi bi-shield-lock-fill text-warning"></i>
                <span>Admin Command Center</span>
              </button>
            )}

            {user ? (
              <div className="dropdown ms-1">
                <button
                  className="btn btn-dark btn-sm rounded-pill px-3 py-2 dropdown-toggle border border-white border-opacity-10 d-flex align-items-center gap-2"
                  type="button"
                  data-bs-toggle="dropdown"
                >
                  <div className="position-relative d-inline-flex align-items-center justify-content-center">
                    <i className="bi bi-person-circle text-cyan fs-6"></i>
                    <span
                      className={`position-absolute top-0 start-100 translate-middle border border-dark rounded-circle ${
                        user.status === 'WARNED' ? 'bg-warning' : (user.status === 'BANNED' ? 'bg-danger' : 'bg-success')
                      }`}
                      style={{ width: 8, height: 8 }}
                      title={`Account Status: ${user.status || 'ACTIVE'}`}
                    ></span>
                  </div>
                  <span className="d-none d-sm-inline fw-medium">{user.fullName || user.username}</span>
                  <span
                    className={`badge d-inline-flex align-items-center gap-1 px-2 py-0.5 small rounded-pill ${
                      user.status === 'WARNED'
                        ? 'bg-warning bg-opacity-20 text-warning border border-warning border-opacity-40'
                        : user.status === 'BANNED'
                        ? 'bg-danger bg-opacity-20 text-danger border border-danger border-opacity-40'
                        : 'bg-success bg-opacity-20 text-success border border-success border-opacity-30'
                    }`}
                  >
                    <span
                      className={`rounded-circle ${
                        user.status === 'WARNED' ? 'bg-warning' : (user.status === 'BANNED' ? 'bg-danger' : 'bg-success')
                      }`}
                      style={{ width: 6, height: 6 }}
                    ></span>
                    {user.status || 'ACTIVE'}
                  </span>
                </button>
                <ul className="dropdown-menu dropdown-menu-dark dropdown-menu-end glass-card border-secondary p-3 shadow-lg" style={{ minWidth: 270 }}>
                  <li className="mb-2">
                    <div className="fw-bold text-white fs-6">{user.fullName || user.username}</div>
                    <span className="text-cyan small font-monospace">@{user.username}</span>
                    <div className="text-muted small text-truncate">{user.email}</div>
                  </li>
                  <li className="mb-2">
                    <div className="p-2.5 rounded bg-dark bg-opacity-60 border border-secondary border-opacity-30">
                      <div className="d-flex justify-content-between align-items-center mb-1">
                        <span className="small text-muted text-uppercase fw-semibold" style={{ fontSize: '0.72rem' }}>Account Status</span>
                        <span
                          className={`badge px-2 py-0.5 small rounded-pill ${
                            user.status === 'WARNED'
                              ? 'bg-warning bg-opacity-20 text-warning border border-warning border-opacity-40'
                              : user.status === 'BANNED'
                              ? 'bg-danger bg-opacity-20 text-danger border border-danger border-opacity-40'
                              : 'bg-success bg-opacity-20 text-success border border-success border-opacity-30'
                          }`}
                        >
                          {user.status || 'ACTIVE'}
                        </span>
                      </div>
                      <div className="d-flex justify-content-between align-items-center">
                        <span className="small text-muted text-uppercase fw-semibold" style={{ fontSize: '0.72rem' }}>Platform Standing</span>
                        <span className="small text-light">
                          {user.status === 'WARNED' ? '⚠️ Advisory Issued' : (user.status === 'BANNED' ? '⛔ Suspended' : '✅ Good Standing')}
                        </span>
                      </div>
                      {isAdmin && (
                        <div className="mt-1 pt-1 border-top border-secondary border-opacity-25 d-flex justify-content-between align-items-center">
                          <span className="small text-warning fw-bold" style={{ fontSize: '0.72rem' }}>Privileges</span>
                          <span className="badge bg-danger bg-opacity-30 text-warning border border-danger border-opacity-40 small">🛡️ SUPERUSER</span>
                        </div>
                      )}
                    </div>
                  </li>
                  <li><hr className="dropdown-divider border-secondary my-2" /></li>
                  <li>
                    <button className="dropdown-item text-danger d-flex align-items-center gap-2 rounded px-2 py-1.5" onClick={logout}>
                      <i className="bi bi-box-arrow-right"></i> Sign Out
                    </button>
                  </li>
                </ul>
              </div>
            ) : (
              <button className="btn btn-cyan-gradient rounded-pill px-4 py-2 ms-1" onClick={onOpenAuth}>
                Sign In / Register
              </button>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}
