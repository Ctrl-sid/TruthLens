import React, { useState, useEffect } from 'react';
import { messagingService } from '../../services/messagingService';

export default function UserMessagingDrawer({ show, onClose, currentUser }) {
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [newMessage, setNewMessage] = useState('');
  const [newSubject, setNewSubject] = useState('');
  const [sending, setSending] = useState(false);
  const [activeTab, setActiveTab] = useState('inbox'); // inbox, compose

  const isAdmin = currentUser && (currentUser.role === 'ROLE_ADMIN' || currentUser.username === 'admin');

  useEffect(() => {
    if (show) {
      loadMessages();
    }
  }, [show]);

  const loadMessages = async () => {
    setLoading(true);
    const data = await messagingService.getUserMessages();
    setMessages(data);
    setLoading(false);
  };

  const handleSend = async (e) => {
    e.preventDefault();
    if (!newMessage.trim()) return;
    setSending(true);

    await messagingService.sendMessageToAdmin(null, newSubject || 'Direct Message to Admin', newMessage, 'User Inquiry');
    setNewMessage('');
    setNewSubject('');
    setSending(false);
    setActiveTab('inbox');
    loadMessages();
  };

  if (!show) return null;

  return (
    <div className="offcanvas offcanvas-end show glass-card text-white border-secondary" tabIndex="-1" style={{ visibility: 'visible', width: '480px', zIndex: 1055 }}>
      <div className="offcanvas-header border-bottom border-secondary">
        <h5 className="offcanvas-title fw-bold brand-font d-flex align-items-center gap-2">
          <i className="bi bi-chat-left-text text-cyan"></i>
          <span>{isAdmin ? 'Admin Dispatches & User Replies' : 'Admin Messaging Gateway'}</span>
        </h5>
        <button type="button" className="btn-close btn-close-white" onClick={onClose}></button>
      </div>

      <div className="offcanvas-body d-flex flex-column">
        {/* User Account Status Indicator Header */}
        {currentUser && (
          <div className="d-flex justify-content-between align-items-center mb-3 bg-dark bg-opacity-50 p-2.5 rounded border border-secondary border-opacity-25 small">
            <div className="d-flex align-items-center gap-2">
              <i className="bi bi-person-circle text-cyan"></i>
              <span className="text-white fw-semibold">{currentUser.fullName || currentUser.username}</span>
              <span className="text-muted font-monospace">(@{currentUser.username})</span>
            </div>
            <span
              className={`badge px-2 py-0.5 small rounded-pill ${
                currentUser.status === 'WARNED'
                  ? 'bg-warning bg-opacity-20 text-warning border border-warning border-opacity-40'
                  : currentUser.status === 'BANNED'
                  ? 'bg-danger bg-opacity-20 text-danger border border-danger border-opacity-40'
                  : 'bg-success bg-opacity-20 text-success border border-success border-opacity-30'
              }`}
            >
              Standing: {currentUser.status || 'ACTIVE'}
            </span>
          </div>
        )}

        {/* Navigation - Hide compose tab if user is Admin */}
        <ul className="nav nav-pills mb-3 gap-2 border-bottom border-secondary border-opacity-25 pb-2">
          <li className="nav-item">
            <button
              className={`nav-link py-1 px-3 small ${activeTab === 'inbox' ? 'active' : ''}`}
              onClick={() => setActiveTab('inbox')}
            >
              <i className="bi bi-inbox me-1"></i> Messages ({messages.length})
            </button>
          </li>
          {!isAdmin && (
            <li className="nav-item">
              <button
                className={`nav-link py-1 px-3 small ${activeTab === 'compose' ? 'active' : ''}`}
                onClick={() => setActiveTab('compose')}
              >
                <i className="bi bi-pencil-square me-1"></i> Contact Admin
              </button>
            </li>
          )}
        </ul>

        {activeTab === 'inbox' && (
          <div className="flex-grow-1 overflow-auto">
            {loading ? (
              <div className="text-center py-5">
                <span className="spinner-border text-cyan"></span>
              </div>
            ) : messages.length > 0 ? (
              <div className="d-flex flex-column gap-3">
                {messages.map((msg, idx) => {
                  const isAdminSender = msg.senderUsername === 'admin' || msg.senderUsername?.toLowerCase().includes('admin');
                  return (
                    <div
                      key={idx}
                      className={`p-3 rounded-3 border ${
                        isAdminSender
                          ? 'bg-primary bg-opacity-15 border-cyan border-opacity-30'
                          : 'bg-dark bg-opacity-40 border-secondary border-opacity-25'
                      }`}
                    >
                      <div className="d-flex justify-content-between align-items-center mb-1">
                        <span className={`badge ${isAdminSender ? 'bg-cyan text-dark fw-bold' : 'bg-secondary text-light'} px-2 py-0.5 small`}>
                          {isAdminSender ? '🛡️ Admin Superuser' : (msg.senderFullName || msg.senderUsername)}
                        </span>
                        <span className="small text-muted" style={{ fontSize: '0.75rem' }}>{msg.createdAt}</span>
                      </div>

                      <h6 className="fw-bold text-white mb-1 mt-2">{msg.subject}</h6>

                      {msg.claimContextSummary && (
                        <div className="small text-cyan font-monospace bg-dark bg-opacity-50 p-2 rounded mb-2 border border-cyan border-opacity-20" style={{ fontSize: '0.75rem' }}>
                          Context: {msg.claimContextSummary}
                        </div>
                      )}

                      <p className="small text-light opacity-90 mb-0" style={{ whiteSpace: 'pre-wrap' }}>
                        {msg.messageText}
                      </p>
                    </div>
                  );
                })}
              </div>
            ) : (
              <div className="text-center py-5 text-muted">
                <i className="bi bi-chat-square-dots fs-1 mb-2 d-block opacity-50"></i>
                <p className="mb-0">No messages in gateway.</p>
              </div>
            )}
          </div>
        )}

        {!isAdmin && activeTab === 'compose' && (
          <form onSubmit={handleSend} className="flex-grow-1 d-flex flex-column">
            <div className="mb-3">
              <label className="form-label small text-muted">Subject / Topic</label>
              <input
                type="text"
                className="form-control form-control-dark"
                placeholder="e.g. Question on News Credibility Rules"
                value={newSubject}
                onChange={(e) => setNewSubject(e.target.value)}
                required
              />
            </div>

            <div className="mb-3 flex-grow-1 d-flex flex-column">
              <label className="form-label small text-muted">Message / Knowledge Share</label>
              <textarea
                className="form-control form-control-dark flex-grow-1"
                rows="6"
                placeholder="Type your message to the Admin superuser moderation team..."
                value={newMessage}
                onChange={(e) => setNewMessage(e.target.value)}
                required
              ></textarea>
            </div>

            <button type="submit" className="btn btn-cyan-gradient rounded-pill w-100 py-2 mt-auto" disabled={sending}>
              {sending ? 'Sending Message...' : 'Send Message to Admin Gateway'}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
