import React from 'react';
import { Download, QrCode, Smartphone, ShieldCheck, Check } from 'lucide-react';

export default function DownloadCTA({ onDownloadAndroid, onOpenQR }) {
  return (
    <section className="download-cta-section" id="download">
      <div className="container-fullscreen">
        <div className="download-cta-card">
          <div className="cta-left-content">
            <div className="tag-pill-warm">
              <span className="pulsing-warm-dot"></span>
              <span>GET THE APP</span>
            </div>

            <h2 className="cta-heading-large">
              Get Foundly on your Android phone.
            </h2>

            <p className="cta-subhead-text">
              Fast, simple, and free. Works on Samsung Galaxy, Google Pixel, Xiaomi, OnePlus, and all phones running Android 8 or newer.
            </p>

            {/* Direct Action Buttons */}
            <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', marginTop: '24px' }}>
              <button 
                onClick={onDownloadAndroid}
                className="btn-apple-primary"
                id="cta-direct-download-btn"
                style={{ padding: '13px 26px', fontSize: '0.86rem' }}
              >
                <Download size={16} />
                <span>Download for Android</span>
              </button>

              <button 
                onClick={onOpenQR}
                className="btn-apple-secondary"
                id="cta-scan-qr-btn"
                style={{ padding: '13px 20px', fontSize: '0.86rem' }}
              >
                <QrCode size={16} />
                <span>Scan QR Code</span>
              </button>
            </div>

            {/* Simple spec badges */}
            <div className="cta-spec-badges-grid">
              <div className="cta-spec-item">
                <span className="cta-spec-title">Compatibility</span>
                <span className="cta-spec-val">Android 8.0+</span>
              </div>
              <div className="cta-spec-item">
                <span className="cta-spec-title">Download size</span>
                <span className="cta-spec-val">Under 20 MB</span>
              </div>
              <div className="cta-spec-item">
                <span className="cta-spec-title">Privacy</span>
                <span className="cta-spec-val">No ads or tracking</span>
              </div>
              <div className="cta-spec-item">
                <span className="cta-spec-title">Price</span>
                <span className="cta-spec-val">100% Free</span>
              </div>
            </div>
          </div>

          {/* Right: Clean White QR Code Stand in Warm Brown tones */}
          <div className="cta-qr-stand">
            <div className="qr-stand-card">
              <img 
                src="https://i.postimg.cc/CKkS022d/qr.png" 
                onError={(e) => { e.target.onerror = null; e.target.src = '/assets/qr.png'; }}
                alt="Scan QR Code to Download Foundly" 
                style={{ 
                  width: '160px', 
                  height: '160px', 
                  objectFit: 'contain', 
                  borderRadius: '12px',
                  display: 'block',
                  margin: '0 auto'
                }} 
              />

              <div style={{ textAlign: 'center', marginTop: '12px' }}>
                <div style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                  Point Phone Camera
                </div>
                <div style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)', marginTop: '2px' }}>
                  Scan to install directly on your phone
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
