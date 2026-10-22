import React from 'react';
import { Smartphone, Shield, ArrowUpRight } from 'lucide-react';

export default function Navbar({ onOpenDownload, onOpenReport }) {
  return (
    <header className="editorial-navbar">
      <a href="#" className="brand-logo" id="nav-brand-logo">
        FOUNDIT
        <span className="brand-badge">LOST & FOUND</span>
      </a>

      <nav>
        <ul className="nav-links">
          <li><a href="#about" className="nav-link">About</a></li>
          <li><a href="#categories" className="nav-link">Categories</a></li>
          <li><a href="#android" className="nav-link">Android App</a></li>
          <li><a href="#feed" className="nav-link">Live Radar</a></li>
          <li><a href="#safety" className="nav-link">Safety Protocol</a></li>
        </ul>
      </nav>

      <div className="nav-right-actions">
        <button 
          className="lang-switch" 
          title="Toggle Language"
          onClick={() => alert('Language set to English (EN). Multi-lingual support active.')}
        >
          EN &nbsp;·&nbsp; DA
        </button>

        <button 
          onClick={onOpenReport}
          className="btn-outline"
          id="nav-report-btn"
          style={{ padding: '8px 16px', fontSize: '0.72rem' }}
        >
          Report Item
        </button>

        <button 
          onClick={onOpenDownload}
          className="btn-terracotta"
          id="nav-download-app-btn"
          style={{ padding: '10px 18px', fontSize: '0.72rem' }}
        >
          <Smartphone size={15} />
          <span>Get Android App</span>
        </button>
      </div>
    </header>
  );
}
