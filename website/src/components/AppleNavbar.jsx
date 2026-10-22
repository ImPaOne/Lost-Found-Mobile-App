import React from 'react';
import { Download, QrCode } from 'lucide-react';

export default function AppleNavbar({ onDownloadApk, onOpenQR }) {
  return (
    <header className="apple-navbar" id="overview">
      <div className="container-fullscreen apple-nav-inner">
        <a href="#overview" className="apple-logo">
          <span>FOUNDIT</span>
          <span className="apple-logo-badge">Android Native</span>
        </a>

        <nav>
          <ul className="apple-nav-links">
            <li><a href="#overview" className="apple-nav-link">Overview</a></li>
            <li><a href="#pillars" className="apple-nav-link">Architecture</a></li>
            <li><a href="#directory" className="apple-nav-link">Live Registry</a></li>
            <li><a href="#download" className="apple-nav-link">Get Client</a></li>
          </ul>
        </nav>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <button 
            onClick={onOpenQR}
            className="btn-apple-secondary"
            id="nav-qr-btn"
            style={{ padding: '8px 14px', fontSize: '0.78rem' }}
            title="Scan QR Code to install on Android phone"
          >
            <QrCode size={14} />
            <span>Scan QR</span>
          </button>

          <button 
            onClick={onDownloadApk}
            className="btn-apple-primary"
            id="nav-direct-download-btn"
            style={{ padding: '8px 18px', fontSize: '0.78rem' }}
          >
            <Download size={14} />
            <span>Download APK</span>
          </button>
        </div>
      </div>
    </header>
  );
}
