import React from 'react';
import { Smartphone, Download, ArrowUp, Shield } from 'lucide-react';

export default function AppleFooter({ onDownloadApk }) {
  const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <footer className="apple-footer">
      <div className="container-fullscreen">
        <div className="apple-footer-grid">
          {/* Brand Col */}
          <div>
            <div style={{ fontFamily: 'var(--font-serif)', fontWeight: 700, fontSize: '1.4rem', color: 'var(--text-primary)', marginBottom: '8px' }}>
              FOUNDIT
            </div>
            <p style={{ maxWidth: '280px', marginBottom: '16px', lineHeight: '1.6' }}>
              The minimalist Lost &amp; Found application built exclusively for the Android mobile ecosystem.
            </p>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--brown-caramel)', fontWeight: 700, fontSize: '0.76rem' }}>
              <Shield size={14} />
              <span>Verified Package · Universal ARM64</span>
            </div>
          </div>

          {/* Architecture */}
          <div>
            <div className="footer-section-title">Architecture</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              <a href="#pillars" style={{ color: 'inherit', textDecoration: 'none' }}>On-Device AI Vision</a>
              <a href="#pillars" style={{ color: 'inherit', textDecoration: 'none' }}>Zero-Knowledge Proofs</a>
              <a href="#pillars" style={{ color: 'inherit', textDecoration: 'none' }}>Safe Return Lockers</a>
              <a href="#directory" style={{ color: 'inherit', textDecoration: 'none' }}>Live Campus Registry</a>
            </div>
          </div>

          {/* Android Client */}
          <div>
            <div className="footer-section-title">Android Client</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              <span style={{ color: 'var(--text-primary)', fontWeight: 600 }}>v2.4.2 Production</span>
              <span>Android 8.0 - 15 Support</span>
              <span>SHA-256 Checksum</span>
              <button 
                onClick={onDownloadApk} 
                style={{ background: 'none', border: 'none', color: 'var(--brown-caramel)', textAlign: 'left', padding: 0, cursor: 'pointer', fontSize: 'inherit', fontFamily: 'inherit', fontWeight: 700 }}
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
              <span>Lost &amp; Found Ecosystem</span>
            </div>
          </div>
        </div>

        {/* Bottom bar */}
        <div style={{ borderTop: '1px solid var(--border-subtle)', paddingTop: '24px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
          <div>
            © 2026 FOUNDIT · Native Android Client. Designed for calm recovery.
          </div>

          <button 
            onClick={scrollToTop}
            style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.78rem', fontWeight: 600 }}
          >
            <span>Back to top</span>
            <ArrowUp size={13} />
          </button>
        </div>
      </div>
    </footer>
  );
}
