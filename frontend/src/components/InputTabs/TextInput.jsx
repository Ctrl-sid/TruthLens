import React, { useState } from 'react';

export default function TextInput({ onVerify, loading }) {
  const [text, setText] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!text.trim()) return;
    onVerify('TEXT', text);
  };

  return (
    <form onSubmit={handleSubmit}>
      <div className="mb-3 position-relative">
        <label className="form-label text-light fw-semibold d-flex justify-content-between">
          <span>Paste News Headline, Social Post, or Article Text</span>
          <span className="text-muted small">{text.length} characters</span>
        </label>
        <textarea
          className="form-control form-control-dark p-3"
          rows="5"
          placeholder="e.g. Breaking: NASA James Webb Space Telescope discovers liquid water clouds on exoplanet LHS 1140b..."
          value={text}
          onChange={(e) => setText(e.target.value)}
          required
        ></textarea>
      </div>

      <div className="d-flex justify-content-between align-items-center">
        <div className="d-flex gap-2">
          <button
            type="button"
            className="btn btn-outline-secondary btn-sm rounded-pill text-light opacity-75"
            onClick={() => setText('NASA James Webb Space Telescope discovers atmospheric water vapor on distant exoplanet.')}
          >
            Sample Genuine News
          </button>
          <button
            type="button"
            className="btn btn-outline-secondary btn-sm rounded-pill text-light opacity-75"
            onClick={() => setText('SHOCKING SECRET REVEALED: Anonymous doctor leaks 100% miracle cure for all diseases!')}
          >
            Sample Clickbait Fake
          </button>
        </div>

        <button
          type="submit"
          className="btn btn-cyan-gradient rounded-pill px-4 py-2 d-flex align-items-center gap-2"
          disabled={loading || !text.trim()}
        >
          {loading ? (
            <>
              <span className="spinner-border spinner-border-sm" role="status"></span>
              <span>Running NLP Pipeline...</span>
            </>
          ) : (
            <>
              <i className="bi bi-shield-check fs-5"></i>
              <span>Measure Genuineness</span>
            </>
          )}
        </button>
      </div>
    </form>
  );
}
