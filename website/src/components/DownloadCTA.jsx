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
              <svg width="130" height="130" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect x="5" y="5" width="28" height="28" rx="4" fill="#1E1510" />
                <rect x="9" y="9" width="20" height="20" rx="2" fill="#FFFFFF" />
                <rect x="13" y="13" width="12" height="12" fill="#8C5E3C" />

                <rect x="67" y="5" width="28" height="28" rx="4" fill="#1E1510" />
                <rect x="71" y="9" width="20" height="20" rx="2" fill="#FFFFFF" />
                <rect x="75" y="13" width="12" height="12" fill="#8C5E3C" />

                <rect x="5" y="67" width="28" height="28" rx="4" fill="#1E1510" />
                <rect x="9" y="71" width="20" height="20" rx="2" fill="#FFFFFF" />
                <rect x="13" y="75" width="12" height="12" fill="#8C5E3C" />

                <rect x="38" y="10" width="8" height="8" fill="#1E1510" />
                <rect x="50" y="10" width="10" height="8" fill="#8C5E3C" />
                <rect x="38" y="24" width="22" height="8" fill="#1E1510" />
                
                <rect x="10" y="38" width="8" height="16" fill="#1E1510" />
                <rect x="24" y="42" width="12" height="8" fill="#B69372" />
                <rect x="42" y="38" width="16" height="16" rx="3" fill="#8C5E3C" />
                <rect x="64" y="38" width="8" height="8" fill="#1E1510" />
                <rect x="78" y="44" width="16" height="10" fill="#1E1510" />

                <rect x="38" y="67" width="10" height="8" fill="#1E1510" />
                <rect x="52" y="67" width="14" height="18" fill="#1E1510" />
                <rect x="72" y="67" width="22" height="8" fill="#8C5E3C" />
                <rect x="72" y="81" width="10" height="12" fill="#1E1510" />
                <rect x="38" y="81" width="8" height="12" fill="#1E1510" />
              </svg>

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
