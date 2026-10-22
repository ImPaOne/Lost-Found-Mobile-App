import React from 'react';
import { Smartphone, Download, QrCode, ShieldCheck } from 'lucide-react';

export default function AppleNavbar({ onDownloadApk, onOpenQR }) {
  return (
    <header className="apple-navbar">
      <div className="container-apple apple-nav-inner">
        <a href="#" className="apple-logo">
          <span>FOUNDIT</span>
          <span className="apple-logo-badge">Android</span>
        </a>

        <nav>
          <ul className="apple-nav-links">
            <li><a href="#overview" className="apple-nav-link">Overview</a></li>
            <li><a href="#features" className="apple-nav-link">Features</a></li>
            <li><a href="#radar-demo" className="apple-nav-link">Interactive Radar</a></li>
            <li><a href="#specs" className="apple-nav-link">Tech Specs</a></li>
          </ul>
        </nav>

        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <button 
            onClick={onOpenQR}
            className="btn-apple-secondary"
            id="nav-qr-btn"
            style={{ padding: '8px 14px', fontSize: '0.8rem' }}
            title="Scan QR Code to install on Android phone"
          >
            <QrCode size={15} />
            <span style={{ display: 'none', sm: 'inline' }}>Scan QR</span>
          </button>

          <button 
            onClick={onDownloadApk}
            className="btn-apple-primary"
            id="nav-direct-download-btn"
            style={{ padding: '8px 18px', fontSize: '0.8rem' }}
          >
            <Download size={15} />
            <span>Download APK</span>
          </button>
        </div>
      </div>
    </header>
  );
}
