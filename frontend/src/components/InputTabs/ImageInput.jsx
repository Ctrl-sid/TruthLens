import React, { useState } from 'react';

export default function ImageInput({ onVerify, loading }) {
  const [selectedImage, setSelectedImage] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);

  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      setSelectedImage(file);
      const reader = new FileReader();
      reader.onloadend = () => {
        setImagePreview(reader.result);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleSampleClick = (sampleType) => {
    if (sampleType === 'genuine') {
      const sample = 'NASA James Webb Space Telescope Discovers Atmospheric Water Vapor';
      setImagePreview('https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&auto=format&fit=crop');
      setSelectedImage(sample);
    } else {
      const sample = 'SECRET MIRACLE CURE REVEALED BY ANONYMOUS DOCTORS!';
      setImagePreview('https://images.unsplash.com/photo-1584515979956-d9f6e5d09982?w=600&auto=format&fit=crop');
      setSelectedImage(sample);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!selectedImage) return;
    const contentStr = typeof selectedImage === 'string' ? selectedImage : (imagePreview || selectedImage.name);
    onVerify('IMAGE', contentStr);
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
              <p className="text-muted small mb-0">Supports PNG, JPG, WEBP (OCR & Visual Artifact Scanning)</p>
            </div>
          )}
        </div>
      </div>

      <div className="d-flex justify-content-between align-items-center">
        <div className="d-flex gap-2">
          <button
            type="button"
            className="btn btn-outline-secondary btn-sm rounded-pill text-light opacity-75"
            onClick={() => handleSampleClick('genuine')}
          >
            Sample Authentic News Screenshot
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
          disabled={loading || !selectedImage}
        >
          {loading ? (
            <>
              <span className="spinner-border spinner-border-sm" role="status"></span>
              <span>Running OCR & Digital Forensics...</span>
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
