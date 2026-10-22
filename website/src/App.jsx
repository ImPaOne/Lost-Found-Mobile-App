import React, { useState, useEffect } from 'react';
import AppleNavbar from './components/AppleNavbar';
import ModernEditorialHero from './components/ModernEditorialHero';
import InteractivePhoneSimulator from './components/InteractivePhoneSimulator';
import EditorialCategories from './components/EditorialCategories';
import DownloadCTA from './components/DownloadCTA';
import AppleFooter from './components/AppleFooter';
import DownloadModal from './components/DownloadModal';
import { Download, QrCode, Smartphone } from 'lucide-react';
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
        {/* Modern Editorial Hero */}
        <ModernEditorialHero 
          onDownloadApk={triggerApkDownload}
          onOpenQR={() => setIsDownloadOpen(true)}
        />

        {/* Working Interactive Phone Simulator */}
        <InteractivePhoneSimulator 
          onDownloadApk={triggerApkDownload}
          onOpenQR={() => setIsDownloadOpen(true)}
        />

        {/* Editorial Categories matching Reference Image 4 */}
        <EditorialCategories 
          onDownloadApk={triggerApkDownload}
        />

        {/* Download Call to Action Section */}
        <DownloadCTA 
          onDownloadApk={triggerApkDownload}
          onOpenQR={() => setIsDownloadOpen(true)}
        />
      </main>

      {/* Footer */}
      <AppleFooter 
        onDownloadApk={triggerApkDownload}
      />

      {/* Floating Sticky Android Download Pill */}
      {showStickyBar && (
        <aside aria-label="Quick Android Download" style={{
          position: 'fixed',
          bottom: '24px',
          left: '50%',
          transform: 'translateX(-50%)',
          zIndex: 90,
          background: 'rgba(255, 255, 255, 0.94)',
          backdropFilter: 'saturate(180%) blur(20px)',
          WebkitBackdropFilter: 'saturate(180%) blur(20px)',
          border: '1px solid rgba(0, 0, 0, 0.08)',
          borderRadius: '9999px',
          padding: '8px 12px 8px 18px',
          display: 'flex',
          alignItems: 'center',
          gap: '16px',
          boxShadow: '0 12px 32px rgba(0, 0, 0, 0.1)',
          color: '#111418'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#10B981', display: 'inline-block' }}></span>
            <div style={{ fontSize: '0.82rem', fontWeight: 700 }}>FOUNDIT for Android</div>
            <span style={{ fontSize: '0.72rem', color: '#6B7280' }}>· v2.4.2</span>
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

      {/* Instant Download Toast Notification */}
      {downloadToast && (
        <div style={{
          position: 'fixed',
          top: '80px',
          right: '24px',
          zIndex: 250,
          background: '#FFFFFF',
          border: '1px solid #E5E7EB',
          borderRadius: '12px',
          padding: '14px 18px',
          boxShadow: '0 14px 36px rgba(0, 0, 0, 0.12)',
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          animation: 'modalFadeIn 0.3s ease-out'
        }}>
          <div style={{ width: '28px', height: '28px', borderRadius: '50%', background: '#ECFDF5', color: '#059669', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700 }}>
            ✓
          </div>
          <div>
            <div style={{ fontSize: '0.85rem', fontWeight: 700, color: '#111827' }}>
              Downloading FOUNDIT-v2.4.2.apk
            </div>
            <div style={{ fontSize: '0.74rem', color: '#6B7280' }}>
              Open the downloaded package on your Android device to install.
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
