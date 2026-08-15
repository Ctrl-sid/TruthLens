import React from 'react';

export default function SampleNewsCarousel({ onSelectPreset }) {
  const presets = [
    {
      title: 'NASA James Webb Telescope Discovery',
      category: 'Science & Space',
      badge: 'Genuine (96%)',
      badgeColor: '#10B981',
      text: 'NASA James Webb Space Telescope discovers atmospheric water vapor on distant exoplanet LHS 1140b.',
      type: 'TEXT'
    },
    {
      title: 'Viral Deepfake Voice & Photo Scam',
      category: 'Social Media / Deepfake',
      badge: 'Fake (14%)',
      badgeColor: '#EF4444',
      text: 'Leaked Audio & Photo of World Bank CEO claiming all national debts will be erased by Friday!',
      type: 'TEXT'
    },
    {
      title: 'Sensational Miracle Health Cure',
      category: 'Health Misinformation',
      badge: 'Fabricated (18%)',
      badgeColor: '#EF4444',
      text: 'SHOCKING SECRET REVEALED: Anonymous doctor leaks 100% miracle cure for all diseases that big pharma hid!',
      type: 'TEXT'
    },
    {
      title: 'WHO Global Health Advisory Update',
      category: 'Official Advisory',
      badge: 'Genuine (88%)',
      badgeColor: '#10B981',
      text: 'World Health Organization publishes updated guidance on seasonal viral mitigation and vaccination research.',
      type: 'TEXT'
    }
  ];

  return (
    <div id="presets" className="py-4">
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h5 className="fw-bold text-white mb-0 d-flex align-items-center gap-2">
          <i className="bi bi-lightning-charge text-cyan"></i>
          <span>Interactive Test Presets & Case Studies</span>
        </h5>
        <span className="small text-muted">Click any preset to run instant analysis</span>
      </div>

      <div className="row g-3">
        {presets.map((preset, idx) => (
          <div key={idx} className="col-md-6 col-lg-3">
            <div
              className="glass-card p-3 h-100 cursor-pointer border-0"
              style={{ background: 'rgba(15, 23, 42, 0.5)' }}
              onClick={() => onSelectPreset(preset.type, preset.text)}
            >
              <div className="d-flex justify-content-between align-items-center mb-2">
                <span className="small text-muted">{preset.category}</span>
                <span
                  className="badge px-2 py-1 small rounded-pill"
                  style={{ backgroundColor: `${preset.badgeColor}22`, color: preset.badgeColor, border: `1px solid ${preset.badgeColor}` }}
                >
                  {preset.badge}
                </span>
              </div>

              <h6 className="fw-bold text-white mb-2" style={{ fontSize: '0.95rem' }}>
                {preset.title}
              </h6>

              <p className="small text-muted mb-3 line-clamp-2" style={{ fontSize: '0.825rem' }}>
                "{preset.text}"
              </p>

              <button className="btn btn-outline-secondary btn-sm w-100 rounded-pill text-cyan border-cyan border-opacity-25 opacity-90">
                Run Test <i className="bi bi-arrow-right ms-1"></i>
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
