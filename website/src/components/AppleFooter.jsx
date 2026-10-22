import React from 'react';
import { Smartphone, Download, ArrowUp, Shield } from 'lucide-react';

export default function AppleFooter({ onDownloadApk }) {
  const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <footer className="apple-footer">
      <div className="container-apple">
        <div className="apple-footer-grid">
          {/* Brand Col */}
          <div>
            <div style={{ fontWeight: 800, fontSize: '1.1rem', color: 'var(--text-primary)', marginBottom: '8px' }}>
              FOUNDIT
            </div>
            <p style={{ maxWidth: '300px', marginBottom: '16px' }}>
              The modern Lost &amp; Found application built exclusively for the Android mobile ecosystem.
            </p>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#279A67', fontWeight: 600, fontSize: '0.78rem' }}>
              <Shield size={14} />
              <span>Verified Android Package · Universal ABI</span>
            </div>
          </div>

          {/* Core Features */}
          <div>
            <div className="footer-section-title">Features</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              <a href="#radar-demo" style={{ color: 'inherit', textDecoration: 'none' }}>BLE Proximity Radar</a>
              <a href="#features" style={{ color: 'inherit', textDecoration: 'none' }}>AI Visual Recognition</a>
              <a href="#features" style={{ color: 'inherit', textDecoration: 'none' }}>Anonymous Handover</a>
              <a href="#features" style={{ color: 'inherit', textDecoration: 'none' }}>Printable QR Tags</a>
            </div>
          </div>

          {/* Android Releases */}
          <div>
            <div className="footer-section-title">Android Client</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              <a href="#specs" style={{ color: 'inherit', textDecoration: 'none' }}>Release v2.4.2 (Stable)</a>
              <a href="#specs" style={{ color: 'inherit', textDecoration: 'none' }}>Android 8.0 - 15 Support</a>
              <a href="#specs" style={{ color: 'inherit', textDecoration: 'none' }}>SHA-256 Checksum</a>
              <button 
                onClick={onDownloadApk} 
                style={{ background: 'none', border: 'none', color: 'var(--apple-blue)', textAlign: 'left', padding: 0, cursor: 'pointer', fontSize: 'inherit', fontFamily: 'inherit' }}
              >
                Download APK (18.4 MB)
              </button>
            </div>
          </div>

          {/* Course Work / Project */}
          <div>
            <div className="footer-section-title">Course Work</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              <span>HDSE 26.1 · MAD Project</span>
              <span>Mobile Application Development</span>
              <span>Lost &amp; Found Mobile System</span>
            </div>
          </div>
        </div>

        {/* Bottom bar */}
        <div style={{ borderTop: '1px solid var(--border-subtle)', paddingTop: '24px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
          <div>
            © 2026 FOUNDIT · All rights reserved. Designed for Android.
          </div>

          <button 
            onClick={scrollToTop}
            style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.78rem' }}
          >
            <span>Back to top</span>
            <ArrowUp size={13} />
          </button>
        </div>
      </div>
    </footer>
  );
}
