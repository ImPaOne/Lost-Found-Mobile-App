import React, { useState, useEffect } from 'react';
import { Download, QrCode } from 'lucide-react';

export default function AppleNavbar({ onDownloadAndroid, onOpenQR }) {
  const [isVisible, setIsVisible] = useState(true);
  const [lastScrollY, setLastScrollY] = useState(0);

  useEffect(() => {
    const handleScroll = () => {
      const currentScrollY = window.scrollY;
      
      // Always show near the top of the page
      if (currentScrollY < 60) {
        setIsVisible(true);
      } else if (currentScrollY > lastScrollY && currentScrollY > 120) {
        // Scrolling down -> hide navbar
        setIsVisible(false);
      } else if (currentScrollY < lastScrollY) {
        // Scrolling up -> show navbar
        setIsVisible(true);
      }
      
      setLastScrollY(currentScrollY);
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, [lastScrollY]);

  return (
    <header className={`apple-navbar-floating ${!isVisible ? 'nav-hidden' : ''}`} id="overview">
      <a href="#overview" className="apple-logo">
        <span>Foundly</span>
        <span className="apple-logo-badge">Android</span>
      </a>

      <nav>
        <ul className="apple-nav-links">
          <li><a href="#features" className="apple-nav-link">How it works</a></li>
          <li><a href="#recent" className="apple-nav-link">Recent items</a></li>
          <li><a href="#download" className="apple-nav-link">Get the app</a></li>
        </ul>
      </nav>

      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
        <button 
          onClick={onOpenQR}
          className="btn-apple-secondary nav-btn-compact"
          id="nav-qr-btn"
          title="Scan QR Code to install on Android phone"
        >
          <QrCode size={14} />
          <span>Scan QR</span>
        </button>

        <button 
          onClick={onDownloadAndroid}
          className="btn-apple-primary nav-btn-compact"
          id="nav-direct-download-btn"
        >
          <Download size={14} />
          <span>Download for Android</span>
        </button>
      </div>
    </header>
  );
}
