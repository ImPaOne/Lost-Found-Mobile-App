import React from 'react';
import { Smartphone, Shield, ArrowUp } from 'lucide-react';

export default function Footer({ onOpenDownload }) {
  const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <footer className="editorial-footer">
      <div className="footer-top-grid">
        {/* Brand Column */}
        <div>
          <div className="brand-logo" style={{ marginBottom: '14px' }}>
            FOUNDIT
            <span className="brand-badge">ANDROID ECOSYSTEM</span>
          </div>
          <p style={{ fontSize: '0.84rem', color: 'var(--text-muted)', lineHeight: '1.6', maxWidth: '320px', marginBottom: '20px' }}>
            The decentralized lost and found network engineered to reunite people with their most valuable belongings quickly, safely, and anonymously.
          </p>
          <button 
            onClick={onOpenDownload}
            className="btn-terracotta"
            style={{ padding: '8px 16px', fontSize: '0.72rem' }}
          >
            <Smartphone size={14} />
            <span>Download Android App</span>
          </button>
        </div>

        {/* Directory Links */}
        <div>
          <div className="footer-col-title">Categories</div>
          <ul className="footer-link-list">
            <li><a href="#categories" className="footer-link">Smartphones & Tech</a></li>
            <li><a href="#categories" className="footer-link">Wallets & Keys</a></li>
            <li><a href="#categories" className="footer-link">Pets & Companions</a></li>
            <li><a href="#categories" className="footer-link">Luggage & Backpacks</a></li>
            <li><a href="#feed" className="footer-link">All Live Reports</a></li>
          </ul>
        </div>

        {/* Android Client Tech Specs */}
        <div>
          <div className="footer-col-title">Android Client</div>
          <ul className="footer-link-list">
            <li><a href="#android" className="footer-link">APK Direct Download (v2.4.2)</a></li>
            <li><a href="#android" className="footer-link">Google Play Store Status</a></li>
            <li><a href="#android" className="footer-link">Bluetooth BLE Radar Specs</a></li>
            <li><a href="#android" className="footer-link">SHA-256 Checksum Log</a></li>
            <li><a href="#android" className="footer-link">Android 8.0 - 15 Support</a></li>
          </ul>
        </div>

        {/* Safety & Protocol */}
        <div>
          <div className="footer-col-title">Safety & Trust</div>
          <ul className="footer-link-list">
            <li><a href="#about" className="footer-link">Zero Data Exposure</a></li>
            <li><a href="#about" className="footer-link">Proof-of-Ownership</a></li>
            <li><a href="#about" className="footer-link">Campus Safe Zones</a></li>
            <li><a href="#about" className="footer-link">Verified Finder Rewards</a></li>
            <li><a href="#about" className="footer-link">Privacy Policy</a></li>
          </ul>
        </div>
      </div>

      {/* Bottom Bar */}
      <div className="footer-bottom-bar">
        <div>
          © 2026 FOUNDIT Network · All rights reserved · Built for Mobile App Development Coursework
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
          <span style={{ fontSize: '0.72rem', color: 'var(--text-light)' }}>
            Encrypted End-to-End
          </span>
          <button 
            onClick={scrollToTop}
            className="btn-outline"
            style={{ padding: '6px 12px', fontSize: '0.7rem' }}
          >
            <span>Back to top</span>
            <ArrowUp size={12} />
          </button>
        </div>
      </div>
    </footer>
  );
}
