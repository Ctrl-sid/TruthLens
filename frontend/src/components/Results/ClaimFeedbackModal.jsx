import React, { useState } from 'react';
import { messagingService } from '../../services/messagingService';

export default function ClaimFeedbackModal({ show, onClose, claimResult }) {
  const [rating, setRating] = useState(5);
  const [flagReason, setFlagReason] = useState('NONE');
  const [comments, setComments] = useState('');
  const [subject, setSubject] = useState('');
  const [loading, setLoading] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  if (!show || !claimResult) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    const msgSubject = subject || `Feedback / Context on Result #${claimResult.id}`;
    const claimContext = `${claimResult.verdict} (${claimResult.genuinenessScore}%): "${claimResult.claimSummary}"`;

    await messagingService.submitFeedback(
      claimResult.id,
      rating,
      flagReason !== 'NONE' ? flagReason : null,
      comments
    );

    if (comments.trim() || flagReason !== 'NONE') {
      await messagingService.sendMessageToAdmin(
        claimResult.id,
        msgSubject,
        comments || 'Submitted rating and flag feedback.',
        claimContext
      );
    }

    setLoading(false);
    setSubmitted(true);

    setTimeout(() => {
      setSubmitted(false);
      setComments('');
      setFlagReason('NONE');
      onClose();
    }, 1800);
  };

  return (
    <div className="modal fade show d-block" style={{ backgroundColor: 'rgba(0, 0, 0, 0.75)', backdropFilter: 'blur(8px)', zIndex: 1065 }} tabIndex="-1">
      <div className="modal-dialog modal-dialog-centered modal-lg">
        <div className="modal-content glass-card text-white border-secondary">
          <div className="modal-header border-secondary">
            <h5 className="modal-title fw-bold brand-font d-flex align-items-center gap-2">
              <i className="bi bi-chat-left-dots text-cyan"></i>
              <span>Share Knowledge & Rate Result</span>
            </h5>
            <button type="button" className="btn-close btn-close-white" onClick={onClose}></button>
          </div>

          {submitted ? (
            <div className="modal-body py-5 text-center">
              <i className="bi bi-check-circle-fill text-success display-3 mb-3 d-block"></i>
              <h4 className="fw-bold text-white">Thank You for Your Feedback!</h4>
              <p className="text-muted">Your domain knowledge and rating have been sent to the TruthLens Admin Gateway.</p>
            </div>
          ) : (
            <form onSubmit={handleSubmit}>
              <div className="modal-body py-4">
                <div className="bg-dark bg-opacity-50 p-3 rounded-3 border border-secondary border-opacity-25 mb-4">
                  <span className="small text-muted d-block mb-1">Claim Context</span>
                  <h6 className="fw-bold text-cyan mb-1">{claimResult.claimSummary}</h6>
                  <span className="badge bg-secondary bg-opacity-30 text-light me-2">{claimResult.verdict}</span>
                  <span className="small text-muted">{claimResult.genuinenessScore}% Genuineness Rating</span>
                </div>

                {/* 5-Star Rating */}
                <div className="mb-4 text-center">
                  <label className="form-label small text-muted text-uppercase fw-bold d-block mb-2">
                    Rate the Accuracy & Quality of this Analysis
                  </label>
                  <div className="d-flex justify-content-center gap-2">
                    {[1, 2, 3, 4, 5].map((star) => (
                      <i
                        key={star}
                        className={`bi ${star <= rating ? 'bi-star-fill text-warning' : 'bi-star text-secondary'} fs-2 cursor-pointer transition-all`}
                        onClick={() => setRating(star)}
                        style={{ cursor: 'pointer' }}
                      ></i>
                    ))}
                  </div>
                  <span className="small text-muted mt-1 d-block">{rating} out of 5 Stars</span>
                </div>

                {/* Flag Content Reason */}
                <div className="mb-3">
                  <label className="form-label small text-muted fw-semibold">
                    Report / Flag Content Concerns (Optional)
                  </label>
                  <select
                    className="form-select form-control-dark"
                    value={flagReason}
                    onChange={(e) => setFlagReason(e.target.value)}
                  >
                    <option value="NONE">No Concerns - General Feedback</option>
                    <option value="CULTURALLY_INAPPROPRIATE">Culturally or Personally Inappropriate Result</option>
                    <option value="INACCURATE_FACT">Inaccurate Facts / Outdated Information</option>
                    <option value="PERSONAL_BIAS">Unbalanced Bias or Sensational Misclassification</option>
                    <option value="OTHER">Other Code of Conduct Issue</option>
                  </select>
                </div>

                {/* Subject */}
                <div className="mb-3">
                  <label className="form-label small text-muted fw-semibold">Subject / Message Title</label>
                  <input
                    type="text"
                    className="form-control form-control-dark"
                    placeholder="e.g., Regional Cultural Context on this News Claim"
                    value={subject}
                    onChange={(e) => setSubject(e.target.value)}
                  />
                </div>

                {/* Domain Knowledge & Message Text */}
                <div className="mb-3">
                  <label className="form-label small text-muted fw-semibold">
                    Share Your Domain Knowledge / Detailed Commentary to Admin
                  </label>
                  <textarea
                    className="form-control form-control-dark"
                    rows="4"
                    placeholder="Provide additional links, regional context, or point out why this result may be inappropriate or inaccurate..."
                    value={comments}
                    onChange={(e) => setComments(e.target.value)}
                  ></textarea>
                </div>
              </div>

              <div className="modal-footer border-secondary d-flex justify-content-between">
                <button type="button" className="btn btn-outline-secondary rounded-pill px-3" onClick={onClose}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-cyan-gradient rounded-pill px-4 py-2" disabled={loading}>
                  {loading ? 'Sending to Admin Gateway...' : 'Submit Feedback & Message Admin'}
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
