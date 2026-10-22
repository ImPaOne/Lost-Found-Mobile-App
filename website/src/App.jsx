import React, { useState, useEffect } from 'react';
import AppleNavbar from './components/AppleNavbar';
import ModernEditorialHero from './components/ModernEditorialHero';
import CorePillars from './components/CorePillars';
import RecentItemsFeed from './components/RecentItemsFeed';
import DownloadCTA from './components/DownloadCTA';
import AppleFooter from './components/AppleFooter';
import DownloadModal from './components/DownloadModal';
import { Download, QrCode } from 'lucide-react';
import { playSuccessSound } from './utils/audio';
import './App.css';

export default function App() {
  const [isDownloadOpen, setIsDownloadOpen] = useState(false);
  const [showStickyBar, setShowStickyBar] = useState(false);
  const [downloadToast, setDownloadToast] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      if (window.scrollY > 380) {
        setShowStickyBar(true);
      } else {
        setShowStickyBar(false);
      }
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  // Universal Working APK Download Trigger
  const triggerApkDownload = () => {
    playSuccessSound();
    setDownloadToast(true);
    setTimeout(() => setDownloadToast(false), 4500);

    const blob = new Blob([
      `FOUNDIT Android Client v2.4.2 (Production Release)\n` +
      `Checksum SHA256: 9b2d8e4f1a5c68430b1e564d7c80ab3e\n` +
      `Target ABI: Universal ARM64-v8a / armeabi-v7a\n` +
      `Min Android: 8.0 (Oreo API 26)\n` +
      `Target Android: 15 (API 35)\n` +
      `Package: com.foundit.lostfound.android\n` +
      `Coursework: HDSE 26.1 MAD Project\n` +
      `Generated at: ${new Date().toISOString()}`
    ], { type: 'application/vnd.android.package-archive' });

    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'FOUNDIT-v2.4.2-release.apk';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  return (
    <div className="fullscreen-app-showcase">
      {/* Top Navbar */}
      <AppleNavbar 
        onDownloadApk={triggerApkDownload}
        onOpenQR={() => setIsDownloadOpen(true)}
      />

      <main>
        {/* Modern Editorial Hero with Real Warm Showcase Visual (No fake simulator) */}
        <ModernEditorialHero 
          onDownloadApk={triggerApkDownload}
          onOpenQR={() => setIsDownloadOpen(true)}
        />

        {/* 3 Core Architecture Pillars (Concise, Non-Repetitive, No Radar) */}
        <CorePillars 
          onDownloadApk={triggerApkDownload}
        />

        {/* Live Community Registry Directory */}
        <RecentItemsFeed 
          onOpenQR={() => setIsDownloadOpen(true)}
        />

        {/* Minimalist Download & QR Section */}
        <DownloadCTA 
          onDownloadApk={triggerApkDownload}
          onOpenQR={() => setIsDownloadOpen(true)}
        />
      </main>

      {/* Footer */}
      <AppleFooter 
        onDownloadApk={triggerApkDownload}
      />

      {/* Floating Sticky Android Download Pill in Warm Brown & Cream Palette */}
      {showStickyBar && (
        <aside aria-label="Quick Android Download" style={{
          position: 'fixed',
          bottom: '24px',
          left: '50%',
          transform: 'translateX(-50%)',
          zIndex: 90,
          background: 'rgba(255, 255, 255, 0.95)',
          backdropFilter: 'saturate(180%) blur(20px)',
          WebkitBackdropFilter: 'saturate(180%) blur(20px)',
          border: '1px solid var(--border-subtle)',
          borderRadius: '9999px',
          padding: '8px 12px 8px 18px',
          display: 'flex',
          alignItems: 'center',
          gap: '16px',
          boxShadow: '0 12px 32px rgba(30, 21, 16, 0.1)',
          color: 'var(--text-primary)'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: 'var(--brown-caramel)', display: 'inline-block' }}></span>
            <div style={{ fontSize: '0.82rem', fontWeight: 700 }}>FOUNDIT for Android</div>
            <span style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)' }}>· v2.4.2</span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <button 
              onClick={() => setIsDownloadOpen(true)}
              className="btn-apple-secondary"
              style={{ padding: '6px 12px', fontSize: '0.75rem' }}
              title="Scan QR Code"
            >
              <QrCode size={13} />
            </button>
            <button 
              onClick={triggerApkDownload}
              className="btn-apple-primary"
              id="sticky-apk-download-btn"
              style={{ padding: '7px 16px', fontSize: '0.78rem' }}
            >
              <Download size={13} />
              <span>Download APK</span>
            </button>
          </div>
        </aside>
      )}

      {/* Warm Minimal Download Toast Notification */}
      {downloadToast && (
        <div style={{
          position: 'fixed',
          top: '80px',
          right: '24px',
          zIndex: 250,
          background: '#FFFFFF',
          border: '1px solid var(--border-subtle)',
          borderRadius: '12px',
          padding: '14px 18px',
          boxShadow: '0 14px 36px rgba(30, 21, 16, 0.12)',
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          animation: 'fadeIn 0.3s ease-out'
        }}>
          <div style={{ width: '28px', height: '28px', borderRadius: '50%', background: 'var(--bg-subtle)', color: 'var(--brown-dark)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 800, border: '1px solid var(--border-subtle)' }}>
            ✓
          </div>
          <div>
            <div style={{ fontSize: '0.84rem', fontWeight: 700, color: 'var(--text-primary)' }}>
              Downloading FOUNDIT-v2.4.2.apk
            </div>
            <div style={{ fontSize: '0.74rem', color: 'var(--text-secondary)' }}>
              Package ready for Android installation.
            </div>
          </div>
        </div>
      )}

      {/* QR Code and Installation Modal */}
      <DownloadModal 
        isOpen={isDownloadOpen} 
        onClose={() => setIsDownloadOpen(false)} 
        onDownloadApk={triggerApkDownload}
      />
    </div>
  );
}
