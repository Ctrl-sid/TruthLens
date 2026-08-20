import React, { useState } from 'react';
import Tesseract from 'tesseract.js';

export default function ImageInput({ onVerify, loading }) {
  const [selectedImage, setSelectedImage] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [imageHeadline, setImageHeadline] = useState('');
  const [ocrLoading, setOcrLoading] = useState(false);
  const [ocrProgress, setOcrProgress] = useState(0);
  const [ocrStatus, setOcrStatus] = useState('');

  const runOcrOnImage = async (imageSource) => {
    setOcrLoading(true);
    setOcrProgress(0);
    setOcrStatus('Initializing Tesseract OCR neural engine...');

    try {
      const result = await Tesseract.recognize(imageSource, 'eng', {
        logger: (m) => {
          if (m.status === 'recognizing text') {
            const prog = Math.round((m.progress || 0) * 100);
            setOcrProgress(prog);
            setOcrStatus(`Extracting text from image (${prog}%)...`);
          } else {
            setOcrStatus(m.status);
          }
        },
      });

      const rawText = result?.data?.text || '';
      const cleanText = rawText
        .split('\n')
        .map((line) => line.trim())
        .filter((line) => line.length > 0)
        .join(' ')
        .replace(/\s+/g, ' ')
        .trim();

      if (cleanText.length > 0) {
        setImageHeadline(cleanText);
        setOcrStatus(`✅ OCR extracted ${cleanText.length} characters.`);
      } else {
        setOcrStatus('⚠️ No text detected. You can type the headline manually.');
      }
    } catch (err) {
      console.error('Tesseract OCR error:', err);
      setOcrStatus('OCR failed. Please enter the headline manually.');
    } finally {
      setOcrLoading(false);
    }
  };

  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      setSelectedImage(file);
      const reader = new FileReader();
      reader.onloadend = () => {
        const dataUrl = reader.result;
        setImagePreview(dataUrl);
        runOcrOnImage(dataUrl);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleSampleClick = (sampleType) => {
    if (sampleType === 'genuine') {
      const sampleText = 'NASA James Webb Space Telescope Discovers Atmospheric Water Vapor on Exoplanet';
      setImagePreview('https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&auto=format&fit=crop');
      setSelectedImage('https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&auto=format&fit=crop');
      setImageHeadline(sampleText);
      setOcrStatus('Loaded authentic scientific sample.');
    } else {
      const sampleText = 'SECRET MIRACLE CURE REVEALED BY ANONYMOUS DOCTORS IN 24 HOURS!';
      setImagePreview('https://images.unsplash.com/photo-1584515979956-d9f6e5d09982?w=600&auto=format&fit=crop');
      setSelectedImage('https://images.unsplash.com/photo-1584515979956-d9f6e5d09982?w=600&auto=format&fit=crop');
      setImageHeadline(sampleText);
      setOcrStatus('Loaded manipulated headline sample.');
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!selectedImage) return;
    const contentStr = typeof selectedImage === 'string' ? selectedImage : (imagePreview || selectedImage.name);
    onVerify('IMAGE', contentStr, imageHeadline);
  };

  return (
    <form onSubmit={handleSubmit}>
      <div className="mb-3">
        <label className="form-label text-light fw-semibold">
          Upload Screenshot, Newspaper Clipping, or Social Media Image
        </label>

        <div className="glass-card border-dashed p-4 text-center cursor-pointer position-relative mb-3">
          <input
            type="file"
            accept="image/*"
            className="position-absolute top-0 start-0 w-100 h-100 opacity-0 cursor-pointer"
            onChange={handleImageChange}
          />
          {imagePreview ? (
            <div className="d-flex flex-column align-items-center">
              <img src={imagePreview} alt="Preview" className="img-fluid rounded mb-2" style={{ maxHeight: '180px', objectFit: 'cover' }} />
              <span className="small text-cyan">Image Loaded. Click or Drag to replace.</span>
            </div>
          ) : (
            <div className="py-3">
              <i className="bi bi-cloud-arrow-up fs-1 text-cyan mb-2 d-block"></i>
              <h6 className="fw-semibold text-white">Drag and drop news screenshot here</h6>
              <p className="text-muted small mb-0">Supports PNG, JPG, WEBP (Powered by Tesseract OCR Neural Vision)</p>
            </div>
          )}
        </div>

        {/* OCR Progress Indicator */}
        {ocrLoading && (
          <div className="p-3 mb-3 rounded-3 bg-dark bg-opacity-75 border border-cyan border-opacity-30">
            <div className="d-flex justify-content-between align-items-center mb-1">
              <span className="small text-cyan fw-semibold d-flex align-items-center gap-2">
                <span className="spinner-border spinner-border-sm text-cyan" role="status"></span>
                <span>Tesseract OCR Scanning Image...</span>
              </span>
              <span className="small font-monospace text-cyan">{ocrProgress}%</span>
            </div>
            <div className="progress" style={{ height: 6 }}>
              <div
                className="progress-bar bg-cyan progress-bar-striped progress-bar-animated"
                role="progressbar"
                style={{ width: `${ocrProgress}%` }}
              ></div>
            </div>
          </div>
        )}

        {ocrStatus && !ocrLoading && (
          <div className="d-flex align-items-center gap-2 small text-muted mb-3 px-1">
            <i className="bi bi-info-circle text-cyan"></i>
            <span>{ocrStatus}</span>
          </div>
        )}
      </div>

      <div className="mb-4">
        <label className="form-label text-light small fw-semibold d-flex justify-content-between align-items-center">
          <span className="d-flex align-items-center gap-1.5">
            <i className="bi bi-fonts text-cyan"></i>
            <span>Extracted Headline / Claim</span>
            {imageHeadline && (
              <span className="badge bg-cyan bg-opacity-20 text-cyan border border-cyan border-opacity-30 py-0.5 px-2 ms-1 small">
                OCR Auto-Filled
              </span>
            )}
          </span>
          {imageHeadline && (
            <button
              type="button"
              className="btn btn-link btn-sm p-0 text-muted text-decoration-none"
              onClick={() => setImageHeadline('')}
            >
              Clear
            </button>
          )}
        </label>
        <input
          type="text"
          className="form-control bg-dark text-white border-secondary border-opacity-50 rounded-3 py-2 px-3"
          placeholder="Tesseract OCR will automatically extract text from image, or type manually..."
          value={imageHeadline}
          onChange={(e) => setImageHeadline(e.target.value)}
        />
      </div>

      <div className="d-flex justify-content-between align-items-center flex-wrap gap-2">
        <div className="d-flex gap-2">
          <button
            type="button"
            className="btn btn-outline-secondary btn-sm rounded-pill text-light opacity-75"
            onClick={() => handleSampleClick('genuine')}
          >
            Sample Authentic News
          </button>
          <button
            type="button"
            className="btn btn-outline-secondary btn-sm rounded-pill text-light opacity-75"
            onClick={() => handleSampleClick('fake')}
          >
            Sample Manipulated Image
          </button>
        </div>

        <button
          type="submit"
          className="btn btn-cyan-gradient rounded-pill px-4 py-2 d-flex align-items-center gap-2"
          disabled={loading || ocrLoading || !selectedImage}
        >
          {loading ? (
            <>
              <span className="spinner-border spinner-border-sm" role="status"></span>
              <span>Running Deep NLP & Wire Verification...</span>
            </>
          ) : (
            <>
              <i className="bi bi-file-image fs-5"></i>
              <span>Verify Image Genuineness</span>
            </>
          )}
        </button>
      </div>
    </form>
  );
}

