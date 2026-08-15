import React, { useState } from 'react';

export default function UrlInput({ onVerify, loading }) {
  const [url, setUrl] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!url.trim()) return;
    onVerify('URL', url);
  };

  return (
    <form onSubmit={handleSubmit}>
      <div className="mb-4">
        <label className="form-label text-light fw-semibold">
          Enter Article or Social Media Web URL
        </label>
        <div className="input-group">
          <span className="input-group-text bg-dark border-secondary text-cyan">
            <i className="bi bi-link-45deg fs-4"></i>
          </span>
          <input
            type="url"
            className="form-control form-control-dark py-3"
            placeholder="https://www.example.com/news/article-headline-12345"
            value={url}
            onChange={(e) => setUrl(e.target.value)}
            required
          />
        </div>
        <div className="form-text text-muted small mt-2">
          TruthLens will extract metadata, domain reputation ratings, and news wire cross-references automatically.
        </div>
      </div>

      <div className="d-flex justify-content-between align-items-center">
        <button
          type="button"
          className="btn btn-outline-secondary btn-sm rounded-pill text-light opacity-75"
          onClick={() => setUrl('https://www.reuters.com/fact-check/nasa-webb-telescope-discovery-2024')}
        >
          Insert Sample Reuters URL
        </button>

        <button
          type="submit"
          className="btn btn-cyan-gradient rounded-pill px-4 py-2 d-flex align-items-center gap-2"
          disabled={loading || !url.trim()}
        >
          {loading ? (
            <>
              <span className="spinner-border spinner-border-sm" role="status"></span>
              <span>Fetching & Parsing Web URL...</span>
            </>
          ) : (
            <>
              <i className="bi bi-globe fs-5"></i>
              <span>Verify Web Article</span>
            </>
          )}
        </button>
      </div>
    </form>
  );
}
