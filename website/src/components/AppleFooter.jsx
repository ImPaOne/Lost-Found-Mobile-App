import React from 'react';
import { ArrowUp, ShieldCheck } from 'lucide-react';

export default function AppleFooter({ onDownloadAndroid }) {
  const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <footer className="apple-footer">
      <div className="container-fullscreen">
        <div className="apple-footer-grid">
          {/* Brand Col */}
          <div>
            <div style={{ fontFamily: 'var(--font-serif)', fontWeight: 700, fontSize: '1.45rem', color: 'var(--text-primary)', marginBottom: '8px' }}>
              Foundly
            </div>
            <p style={{ maxWidth: '280px', marginBottom: '16px', lineHeight: '1.6' }}>
              The easy lost and found app made for Android.
            </p>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--brown-caramel)', fontWeight: 700, fontSize: '0.76rem' }}>
              <ShieldCheck size={14} />
              <span>Private &amp; Secure</span>
            </div>
          </div>

          {/* How it works */}
          <div>
            <div className="footer-section-title">How it works</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              <a href="#features" style={{ color: 'inherit', textDecoration: 'none' }}>Quick photo search</a>
              <a href="#features" style={{ color: 'inherit', textDecoration: 'none' }}>Private contact</a>
              <a href="#features" style={{ color: 'inherit', textDecoration: 'none' }}>Campus drop spots</a>
              <a href="#recent" style={{ color: 'inherit', textDecoration: 'none' }}>Recent items</a>
            </div>
          </div>

          {/* Android App */}
          <div>
            <div className="footer-section-title">Get the App</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              <span style={{ color: 'var(--text-primary)', fontWeight: 600 }}>Android 8.0 or newer</span>
              <span>Free to download</span>
              <button 
                onClick={onDownloadAndroid} 
                style={{ background: 'none', border: 'none', color: 'var(--brown-caramel)', textAlign: 'left', padding: 0, cursor: 'pointer', fontSize: 'inherit', fontFamily: 'inherit', fontWeight: 700 }}
              >
                Download for Android
              </button>
            </div>
          </div>

          {/* Project Details */}
          <div>
            <div className="footer-section-title">Project</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              <span>HDSE 26.1 MAD Project</span>
              <span>Mobile Application Development</span>
              <span>Lost &amp; Found System</span>
            </div>
          </div>
        </div>

        {/* Bottom bar */}
        <div style={{ borderTop: '1px solid var(--border-subtle)', paddingTop: '24px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
          <div>
            © 2026 Foundly. Built for easy, safe recovery.
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
