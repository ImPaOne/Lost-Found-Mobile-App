import React, { useState, useEffect } from 'react';
import { Download, QrCode, Menu, X, ChevronRight } from 'lucide-react';

export default function AppleNavbar({ onDownloadAndroid, onOpenQR }) {
  const [isVisible, setIsVisible] = useState(true);
  const [lastScrollY, setLastScrollY] = useState(0);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      const currentScrollY = window.scrollY;
      
      // Always show near the top of the page
      if (currentScrollY < 60) {
        setIsVisible(true);
      } else if (currentScrollY > lastScrollY && currentScrollY > 120) {
        // Scrolling down -> hide navbar (only if mobile menu is closed)
        if (!isMobileMenuOpen) {
          setIsVisible(false);
        }
      } else if (currentScrollY < lastScrollY) {
        // Scrolling up -> show navbar
        setIsVisible(true);
      }
      
      setLastScrollY(currentScrollY);
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, [lastScrollY, isMobileMenuOpen]);

  // Automatically close mobile menu when window is resized to desktop width
  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth > 860) {
        setIsMobileMenuOpen(false);
      }
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // Close mobile menu on Escape key
  useEffect(() => {
    if (!isMobileMenuOpen) return;
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') setIsMobileMenuOpen(false);
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isMobileMenuOpen]);

  const closeMenu = () => setIsMobileMenuOpen(false);

  return (
    <>
      <header className={`apple-navbar-floating ${!isVisible && !isMobileMenuOpen ? 'nav-hidden' : ''}`} id="overview">
        <a href="#overview" className="apple-logo" onClick={closeMenu}>
          <span>Foundly</span>
          <span className="apple-logo-badge">Android</span>
        </a>

        {/* Desktop Navigation Links */}
        <nav aria-label="Main Navigation">
          <ul className="apple-nav-links">
            <li><a href="#features" className="apple-nav-link">How it works</a></li>
            <li><a href="#recent" className="apple-nav-link">Recent items</a></li>
            <li><a href="#download" className="apple-nav-link">Get the app</a></li>
          </ul>
        </nav>

        {/* Action Controls */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <button 
            onClick={onOpenQR}
            className="btn-apple-secondary nav-btn-compact nav-qr-desktop"
            id="nav-qr-btn"
            title="Scan QR Code to install on Android phone"
          >
            <QrCode size={14} />
            <span>Scan QR</span>
          </button>

          <button 
            onClick={onDownloadAndroid}
            className="btn-apple-primary nav-btn-compact nav-download-desktop"
            id="nav-direct-download-btn"
            title="Download for Android"
          >
            <Download size={14} />
            <span>Download for Android</span>
          </button>

          {/* Hamburger Menu Toggle Button for Mobile */}
          <button
            type="button"
            onClick={() => setIsMobileMenuOpen(prev => !prev)}
            className="btn-apple-hamburger"
            id="nav-hamburger-toggle"
            aria-label={isMobileMenuOpen ? "Close navigation menu" : "Open navigation menu"}
            aria-expanded={isMobileMenuOpen}
          >
            {isMobileMenuOpen ? <X size={20} /> : <Menu size={20} />}
          </button>
        </div>

        {/* Mobile Dropdown Menu */}
        {isMobileMenuOpen && (
          <div className="mobile-nav-menu" role="dialog" aria-modal="true" aria-label="Mobile Navigation Menu">
            <ul className="mobile-nav-list">
              <li>
                <a href="#features" className="mobile-nav-link" onClick={closeMenu}>
                  <span>How it works</span>
                  <ChevronRight size={16} />
                </a>
              </li>
              <li>
                <a href="#recent" className="mobile-nav-link" onClick={closeMenu}>
                  <span>Recent items</span>
                  <ChevronRight size={16} />
                </a>
              </li>
              <li>
                <a href="#download" className="mobile-nav-link" onClick={closeMenu}>
                  <span>Get the app</span>
                  <ChevronRight size={16} />
                </a>
              </li>
            </ul>

            <div className="mobile-nav-divider" />

            <div className="mobile-nav-actions">
              <button 
                onClick={() => {
                  closeMenu();
                  onDownloadAndroid();
                }}
                className="btn-apple-primary mobile-menu-action-btn"
              >
                <Download size={16} />
                <span>Download for Android</span>
              </button>

              <button 
                onClick={() => {
                  closeMenu();
                  onOpenQR();
                }}
                className="btn-apple-secondary mobile-menu-action-btn"
              >
                <QrCode size={16} />
                <span>Scan QR Code</span>
              </button>
            </div>
          </div>
        )}
      </header>

      {/* Dimmed backdrop when mobile menu is open */}
      {isMobileMenuOpen && (
        <div 
          className="mobile-nav-backdrop" 
          onClick={closeMenu} 
          aria-hidden="true"
        />
      )}
    </>
  );
}
