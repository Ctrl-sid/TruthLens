import React, { useState, useEffect } from 'react';
import { adminService } from '../../services/adminService';

export default function AdminDashboard({ show, onClose, onLaunchAnalyzer }) {
  const [activeCard, setActiveCard] = useState('overview'); // overview, users, inbox, feedback
  const [users, setUsers] = useState([]);
  const [messages, setMessages] = useState([]);
  const [feedback, setFeedback] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

  // Reply Modal state
  const [replyTarget, setReplyTarget] = useState(null);
  const [replyText, setReplyText] = useState('');
  const [sendingReply, setSendingReply] = useState(false);

  // User Action Reason Modal state
  const [actionTargetUser, setActionTargetUser] = useState(null);
  const [actionStatus, setActionStatus] = useState('WARNED');
  const [actionReason, setActionReason] = useState('');
  const [updatingUser, setUpdatingUser] = useState(false);

  useEffect(() => {
    if (show) {
      loadAllAdminData();
    }
  }, [show]);

  const loadAllAdminData = async () => {
    setLoading(true);
    const [uData, mData, fData] = await Promise.all([
      adminService.getUsers(),
      adminService.getAdminInbox(),
      adminService.getAllClaimFeedback()
    ]);
    setUsers(uData);
    setMessages(mData);
    setFeedback(fData);
    setLoading(false);
  };

  const handleUpdateStatus = async (e) => {
    e.preventDefault();
    if (!actionTargetUser) return;
    setUpdatingUser(true);

    await adminService.updateUserStatus(actionTargetUser.id, actionStatus, actionReason);
    setUpdatingUser(false);
    setActionTargetUser(null);
    setActionReason('');
    loadAllAdminData();
  };

  const handleDeleteUser = async (userId) => {
    if (window.confirm('Are you sure you want to remove this user from the platform?')) {
      await adminService.deleteUser(userId);
      loadAllAdminData();
    }
  };

  const handleSendReply = async (e) => {
    e.preventDefault();
    if (!replyTarget || !replyText.trim()) return;
    setSendingReply(true);

    await adminService.replyToUser(
      replyTarget.senderUsername,
      replyTarget.claimId,
      `RE: ${replyTarget.subject}`,
      replyText,
      replyTarget.claimContextSummary
    );

    setSendingReply(false);
    setReplyTarget(null);
    setReplyText('');
    loadAllAdminData();
  };

  if (!show) return null;

  const filteredUsers = users.filter(u =>
    u.username.toLowerCase().includes(searchTerm.toLowerCase()) ||
    u.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (u.fullName && u.fullName.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  const warnedCount = users.filter(u => u.status === 'WARNED').length;
  const bannedCount = users.filter(u => u.status === 'BANNED').length;

  return (
    <div className="modal fade show d-block" style={{ backgroundColor: 'rgba(10, 3, 6, 0.92)', backdropFilter: 'blur(16px)', zIndex: 1060 }} tabIndex="-1">
      <div className="modal-dialog modal-dialog-centered modal-fullscreen p-md-4">
        <div
          className="modal-content text-white border-0 shadow-lg overflow-hidden"
          style={{
            background: 'radial-gradient(circle at 10% 10%, rgba(220, 38, 38, 0.12) 0%, #15080B 60%, #0D0406 100%)',
            border: '1px solid rgba(220, 38, 38, 0.35)',
            borderRadius: '20px'
          }}
        >
          {/* COMMAND CENTER HEADER */}
          <div className="modal-header border-bottom border-danger border-opacity-30 py-3 px-4 bg-black bg-opacity-40">
            <div className="d-flex align-items-center gap-3">
              <div className="p-2 rounded-3 bg-danger bg-opacity-20 border border-danger border-opacity-40">
                <i className="bi bi-shield-lock-fill fs-3 text-danger"></i>
              </div>
              <div>
                <h4 className="fw-extrabold brand-font text-white mb-0 tracking-wide d-flex align-items-center gap-2">
                  TRUTHLENS <span className="text-danger">SUPERUSER COMMAND CENTER</span>
                </h4>
                <span className="small text-muted">Platform Supervision, Code of Conduct & Gateway Moderation</span>
              </div>
            </div>

            <div className="d-flex align-items-center gap-3">
              <button
                type="button"
                className="btn btn-outline-warning rounded-pill px-4 py-2 fw-semibold d-flex align-items-center gap-2"
                onClick={() => {
                  onClose();
                  if (onLaunchAnalyzer) onLaunchAnalyzer();
                }}
              >
                <i className="bi bi-cpu-fill text-warning"></i>
                <span>Launch News Credibility Analyzer</span>
              </button>
              <button type="button" className="btn-close btn-close-white fs-5" onClick={onClose}></button>
            </div>
          </div>

          <div className="modal-body p-4 overflow-auto">
            {loading ? (
              <div className="text-center py-5">
                <div className="spinner-border text-danger" style={{ width: '3rem', height: '3rem' }}></div>
                <h6 className="mt-3 text-muted">Loading Command Center Telemetry...</h6>
              </div>
            ) : (
              <>
                {/* SYSTEM TELEMETRY CARDS */}
                <div className="row g-3 mb-4">
                  <div className="col-md-3">
                    <div className="bg-black bg-opacity-50 p-3 rounded-3 border border-danger border-opacity-20">
                      <span className="small text-muted text-uppercase fw-bold">Total Platform Users</span>
                      <h3 className="fw-extrabold text-white mb-0 mt-1">{users.length}</h3>
                      <span className="small text-muted">Registered Accounts</span>
                    </div>
                  </div>
                  <div className="col-md-3">
                    <div className="bg-black bg-opacity-50 p-3 rounded-3 border border-warning border-opacity-20">
                      <span className="small text-muted text-uppercase fw-bold">Code of Conduct Flags</span>
                      <h3 className="fw-extrabold text-warning mb-0 mt-1">{warnedCount} Warned / {bannedCount} Banned</h3>
                      <span className="small text-muted">Moderation Enforcement</span>
                    </div>
                  </div>
                  <div className="col-md-3">
                    <div className="bg-black bg-opacity-50 p-3 rounded-3 border border-danger border-opacity-20">
                      <span className="small text-muted text-uppercase fw-bold">Gateway Messages</span>
                      <h3 className="fw-extrabold text-white mb-0 mt-1">{messages.length}</h3>
                      <span className="small text-muted">User Disputes & Inquiries</span>
                    </div>
                  </div>
                  <div className="col-md-3">
                    <div className="bg-black bg-opacity-50 p-3 rounded-3 border border-warning border-opacity-20">
                      <span className="small text-muted text-uppercase fw-bold">Community Ratings</span>
                      <h3 className="fw-extrabold text-warning mb-0 mt-1">{feedback.length}</h3>
                      <span className="small text-muted">Cultural & Accuracy Reports</span>
                    </div>
                  </div>
                </div>

                {/* IN-BODY FUNCTIONALITY DIV CARDS */}
                <div className="row g-3 mb-4">
                  <div className="col-md-4">
                    <div
                      className={`p-3.5 rounded-3 cursor-pointer transition-all border ${
                        activeCard === 'users' ? 'bg-danger bg-opacity-20 border-danger' : 'bg-black bg-opacity-40 border-secondary border-opacity-25 hover-border-danger'
                      }`}
                      onClick={() => setActiveCard('users')}
                    >
                      <div className="d-flex align-items-center gap-3">
                        <i className="bi bi-people-fill fs-2 text-danger"></i>
                        <div>
                          <h6 className="fw-bold text-white mb-0">User Moderation & Code of Conduct</h6>
                          <span className="small text-muted">Warn, Ban, Suspend, or Delete Accounts</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div className="col-md-4">
                    <div
                      className={`p-3.5 rounded-3 cursor-pointer transition-all border ${
                        activeCard === 'inbox' ? 'bg-danger bg-opacity-20 border-danger' : 'bg-black bg-opacity-40 border-secondary border-opacity-25 hover-border-danger'
                      }`}
                      onClick={() => setActiveCard('inbox')}
                    >
                      <div className="d-flex align-items-center gap-3">
                        <i className="bi bi-inbox-fill fs-2 text-warning"></i>
                        <div>
                          <h6 className="fw-bold text-white mb-0">Admin Gateway & Dispatch Inbox</h6>
                          <span className="small text-muted">Answer User Inquiries & Reply to Disputes</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div className="col-md-4">
                    <div
                      className={`p-3.5 rounded-3 cursor-pointer transition-all border ${
                        activeCard === 'feedback' ? 'bg-danger bg-opacity-20 border-danger' : 'bg-black bg-opacity-40 border-secondary border-opacity-25 hover-border-danger'
                      }`}
                      onClick={() => setActiveCard('feedback')}
                    >
                      <div className="d-flex align-items-center gap-3">
                        <i className="bi bi-star-half fs-2 text-warning"></i>
                        <div>
                          <h6 className="fw-bold text-white mb-0">Claim Ratings & Cultural Flag Reports</h6>
                          <span className="small text-muted">Review Star Ratings & Inappropriateness Notes</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                {/* ACTIVE CARD FUNCTIONALITY BODY */}
                <div className="bg-black bg-opacity-60 p-4 rounded-3 border border-danger border-opacity-25">
                  {/* CARD 1: USER MODERATION */}
                  {activeCard === 'users' && (
                    <div>
                      <div className="d-flex justify-content-between align-items-center mb-3">
                        <h5 className="fw-bold text-white mb-0 d-flex align-items-center gap-2">
                          <i className="bi bi-shield-shaded text-danger"></i>
                          <span>Platform User Roster & Code of Conduct Enforcement</span>
                        </h5>
                        <input
                          type="text"
                          className="form-control form-control-dark w-auto py-1 px-3"
                          placeholder="Search username or email..."
                          value={searchTerm}
                          onChange={(e) => setSearchTerm(e.target.value)}
                        />
                      </div>

                      <div className="table-responsive">
                        <table className="table table-dark table-hover align-middle border-secondary">
                          <thead>
                            <tr className="text-muted small text-uppercase">
                              <th>User</th>
                              <th>Email</th>
                              <th>Role</th>
                              <th>Status</th>
                              <th>Joined</th>
                              <th className="text-end">Moderation Action</th>
                            </tr>
                          </thead>
                          <tbody>
                            {filteredUsers.map((u) => (
                              <tr key={u.id}>
                                <td>
                                  <div className="fw-bold text-white">{u.fullName || u.username}</div>
                                  <span className="small text-muted font-monospace">@{u.username}</span>
                                </td>
                                <td className="small text-light">{u.email}</td>
                                <td>
                                  <span className={`badge ${u.role === 'ROLE_ADMIN' ? 'bg-danger text-white fw-bold' : 'bg-secondary bg-opacity-30 text-light'}`}>
                                    {u.role}
                                  </span>
                                </td>
                                <td>
                                  <span
                                    className={`badge px-2.5 py-1 ${
                                      u.status === 'ACTIVE'
                                        ? 'bg-success bg-opacity-20 text-success border border-success border-opacity-30'
                                        : u.status === 'WARNED'
                                        ? 'bg-warning bg-opacity-20 text-warning border border-warning border-opacity-30'
                                        : 'bg-danger bg-opacity-20 text-danger border border-danger border-opacity-30'
                                    }`}
                                  >
                                    {u.status || 'ACTIVE'}
                                  </span>
                                </td>
                                <td className="small text-muted">{u.createdAt}</td>
                                <td className="text-end">
                                  {u.role !== 'ROLE_ADMIN' ? (
                                    <div className="d-flex justify-content-end gap-1">
                                      <button
                                        className="btn btn-outline-warning btn-sm py-0.5 px-2"
                                        onClick={() => {
                                          setActionTargetUser(u);
                                          setActionStatus('WARNED');
                                        }}
                                      >
                                        Issue Warning
                                      </button>
                                      <button
                                        className="btn btn-outline-danger btn-sm py-0.5 px-2"
                                        onClick={() => {
                                          setActionTargetUser(u);
                                          setActionStatus('BANNED');
                                        }}
                                      >
                                        Ban / Suspend
                                      </button>
                                      {u.status !== 'ACTIVE' && (
                                        <button
                                          className="btn btn-outline-success btn-sm py-0.5 px-2"
                                          onClick={() => {
                                            setActionTargetUser(u);
                                            setActionStatus('ACTIVE');
                                          }}
                                        >
                                          Reactivate
                                        </button>
                                      )}
                                      <button
                                        className="btn btn-dark text-danger btn-sm py-0.5 px-2 border-danger border-opacity-30"
                                        onClick={() => handleDeleteUser(u.id)}
                                      >
                                        <i className="bi bi-trash"></i>
                                      </button>
                                    </div>
                                  ) : (
                                    <span className="small text-warning font-monospace fw-bold">🛡️ Superuser Protected</span>
                                  )}
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  )}

                  {/* CARD 2: ADMIN GATEWAY INBOX */}
                  {activeCard === 'inbox' && (
                    <div>
                      <h5 className="fw-bold text-white mb-3 d-flex align-items-center gap-2">
                        <i className="bi bi-inbox-fill text-warning"></i>
                        <span>User Dispatches & Knowledge Sharing Submissions</span>
                      </h5>
                      {messages.length > 0 ? (
                        <div className="d-flex flex-column gap-3">
                          {messages.map((msg) => (
                            <div key={msg.id} className="bg-black bg-opacity-40 p-3.5 rounded-3 border border-secondary border-opacity-25">
                              <div className="d-flex justify-content-between align-items-start mb-2">
                                <div>
                                  <span className="fw-bold text-white fs-6">{msg.senderFullName || msg.senderUsername}</span>
                                  <span className="small text-warning ms-2 font-monospace">@{msg.senderUsername}</span>
                                </div>
                                <span className="small text-muted">{msg.createdAt}</span>
                              </div>

                              <h6 className="fw-semibold text-warning mb-1">{msg.subject}</h6>

                              {msg.claimContextSummary && (
                                <div className="small text-danger font-monospace bg-dark bg-opacity-60 p-2 rounded mb-2 border border-danger border-opacity-20">
                                  <i className="bi bi-card-text me-1"></i> Claim Context: {msg.claimContextSummary}
                                </div>
                              )}

                              <p className="small text-light opacity-90 mb-3 bg-black bg-opacity-40 p-3 rounded border border-secondary border-opacity-20" style={{ whiteSpace: 'pre-wrap' }}>
                                {msg.messageText}
                              </p>

                              <div className="d-flex justify-content-end">
                                <button
                                  className="btn btn-outline-warning btn-sm rounded-pill px-3"
                                  onClick={() => setReplyTarget(msg)}
                                >
                                  <i className="bi bi-reply-fill me-1"></i> Reply to User
                                </button>
                              </div>
                            </div>
                          ))}
                        </div>
                      ) : (
                        <div className="text-center py-5 text-muted">
                          <i className="bi bi-inbox fs-1 mb-2 d-block opacity-50"></i>
                          <p>No user dispatches in Admin gateway.</p>
                        </div>
                      )}
                    </div>
                  )}

                  {/* CARD 3: CLAIM RATINGS & FLAGS */}
                  {activeCard === 'feedback' && (
                    <div>
                      <h5 className="fw-bold text-white mb-3 d-flex align-items-center gap-2">
                        <i className="bi bi-star-half text-warning"></i>
                        <span>User Claim Ratings & Inappropriateness Reports</span>
                      </h5>
                      <div className="table-responsive">
                        <table className="table table-dark table-hover align-middle border-secondary">
                          <thead>
                            <tr className="text-muted small text-uppercase">
                              <th>User</th>
                              <th>Rating</th>
                              <th>Flag Reason</th>
                              <th>User Commentary / Knowledge Notes</th>
                              <th>Date</th>
                            </tr>
                          </thead>
                          <tbody>
                            {feedback.map((fb) => (
                              <tr key={fb.id}>
                                <td className="fw-bold text-white font-monospace">@{fb.username}</td>
                                <td>
                                  {fb.rating ? (
                                    <div className="d-flex gap-0.5 text-warning">
                                      {[1, 2, 3, 4, 5].map((s) => (
                                        <i key={s} className={`bi ${s <= fb.rating ? 'bi-star-fill' : 'bi-star text-secondary'}`}></i>
                                      ))}
                                    </div>
                                  ) : (
                                    <span className="text-muted small">N/A</span>
                                  )}
                                </td>
                                <td>
                                  {fb.flagReason ? (
                                    <span className="badge bg-danger bg-opacity-20 text-danger border border-danger border-opacity-30">
                                      {fb.flagReason}
                                    </span>
                                  ) : (
                                    <span className="badge bg-secondary bg-opacity-20 text-light">General Review</span>
                                  )}
                                </td>
                                <td className="small text-light opacity-90">{fb.comments || 'No comment provided'}</td>
                                <td className="small text-muted">{fb.createdAt}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  )}
                </div>
              </>
            )}
          </div>
        </div>
      </div>

      {/* MODERATION REASON MODAL */}
      {actionTargetUser && (
        <div className="modal fade show d-block" style={{ backgroundColor: 'rgba(0, 0, 0, 0.85)', zIndex: 1070 }} tabIndex="-1">
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content glass-card text-white border-danger">
              <div className="modal-header border-danger">
                <h5 className="modal-title fw-bold brand-font text-white">
                  Update Status for @{actionTargetUser.username}
                </h5>
                <button type="button" className="btn-close btn-close-white" onClick={() => setActionTargetUser(null)}></button>
              </div>
              <form onSubmit={handleUpdateStatus}>
                <div className="modal-body py-3">
                  <div className="mb-3">
                    <label className="form-label small text-muted">Selected Status</label>
                    <select
                      className="form-select form-control-dark"
                      value={actionStatus}
                      onChange={(e) => setActionStatus(e.target.value)}
                    >
                      <option value="WARNED">WARNED (Issue Code of Conduct Warning)</option>
                      <option value="BANNED">BANNED (Suspend / Block Authentication)</option>
                      <option value="ACTIVE">ACTIVE (Reactivate Account)</option>
                    </select>
                  </div>

                  <div className="mb-3">
                    <label className="form-label small text-muted">Reason / Code of Conduct Explanation</label>
                    <textarea
                      className="form-control form-control-dark"
                      rows="3"
                      placeholder="Explain code of conduct violation context..."
                      value={actionReason}
                      onChange={(e) => setActionReason(e.target.value)}
                      required
                    ></textarea>
                  </div>
                </div>

                <div className="modal-footer border-danger d-flex justify-content-between">
                  <button type="button" className="btn btn-outline-secondary rounded-pill px-3" onClick={() => setActionTargetUser(null)}>
                    Cancel
                  </button>
                  <button type="submit" className="btn btn-danger rounded-pill px-4" disabled={updatingUser}>
                    {updatingUser ? 'Updating...' : 'Confirm Status Update'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* REPLY MODAL */}
      {replyTarget && (
        <div className="modal fade show d-block" style={{ backgroundColor: 'rgba(0, 0, 0, 0.85)', zIndex: 1070 }} tabIndex="-1">
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content glass-card text-white border-danger">
              <div className="modal-header border-danger">
                <h5 className="modal-title fw-bold brand-font text-white">
                  Reply to @{replyTarget.senderUsername}
                </h5>
                <button type="button" className="btn-close btn-close-white" onClick={() => setReplyTarget(null)}></button>
              </div>
              <form onSubmit={handleSendReply}>
                <div className="modal-body py-3">
                  <div className="bg-black bg-opacity-50 p-2.5 rounded mb-3 border border-secondary border-opacity-25 small">
                    <span className="text-muted d-block">Original Message:</span>
                    <span className="text-light opacity-90">"{replyTarget.messageText}"</span>
                  </div>

                  <div className="mb-3">
                    <label className="form-label small text-muted">Admin Response Dispatch</label>
                    <textarea
                      className="form-control form-control-dark"
                      rows="4"
                      placeholder="Type your response to the user..."
                      value={replyText}
                      onChange={(e) => setReplyText(e.target.value)}
                      required
                    ></textarea>
                  </div>
                </div>

                <div className="modal-footer border-danger d-flex justify-content-between">
                  <button type="button" className="btn btn-outline-secondary rounded-pill px-3" onClick={() => setReplyTarget(null)}>
                    Cancel
                  </button>
                  <button type="submit" className="btn btn-danger rounded-pill px-4" disabled={sendingReply}>
                    {sendingReply ? 'Dispatching...' : 'Send Dispatch Reply'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
