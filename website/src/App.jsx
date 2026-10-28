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

export const APK_DOWNLOAD_URL = "https://download1351.mediafire.com/ll4hx3iaubegyM2xP0I87v5hJqRC_ITjqclnBlh_WJ_tLNKFGkSun8c-nwe-pYyVpr8XNZUv0LtJpCsEZBiuEI0l5YUdp1mysRAKnX03S7KDHrDYISzf80FPmZHE2dFAML6ZU8pScVq3Z7NohH7L8n0JCG36Jh0Z1b9_py32YuJVc-bL/mwtagyhadwo9ssk/app-debug.apk";
export const QR_CODE_IMAGE_URL = "https://i.postimg.cc/GmQmXXNG/Untitled-(1).png";

export default function App() {
  const [isDownloadOpen, setIsDownloadOpen] = useState(false);
  const [showStickyBar, setShowStickyBar] = useState(false);
  const [downloadToast, setDownloadToast] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      if (window.scrollY > 420) {
        setShowStickyBar(true);
      } else {
        setShowStickyBar(false);
      }
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  // Android App Download Trigger -> Redirects directly to the direct APK link
  const triggerAndroidDownload = () => {
    playSuccessSound();
    setDownloadToast(true);
    setTimeout(() => setDownloadToast(false), 4500);

    // Direct redirect to MediaFire APK download
    window.location.href = APK_DOWNLOAD_URL;
  };

  return (
    <div className="fullscreen-app-showcase">
      {/* Floating Auto-Hiding Navbar */}
      <AppleNavbar 
        onDownloadAndroid={triggerAndroidDownload}
        onOpenQR={() => setIsDownloadOpen(true)}
      />

      <main>
        {/* Modern Editorial Hero */}
        <ModernEditorialHero 
          onDownloadAndroid={triggerAndroidDownload}
          onOpenQR={() => setIsDownloadOpen(true)}
        />

        {/* 3 Core How It Works Pillars */}
        <CorePillars 
          onDownloadAndroid={triggerAndroidDownload}
        />

        {/* Recent Items Activity Directory */}
        <RecentItemsFeed 
          onOpenQR={() => setIsDownloadOpen(true)}
        />

        {/* Download & QR Section */}
        <DownloadCTA 
          onDownloadAndroid={triggerAndroidDownload}
          onOpenQR={() => setIsDownloadOpen(true)}
        />
      </main>

      {/* Footer */}
      <AppleFooter 
        onDownloadAndroid={triggerAndroidDownload}
      />

      {/* Floating Sticky Android Download Pill */}
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
            <div style={{ fontSize: '0.82rem', fontWeight: 700 }}>Foundly for Android</div>
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
              onClick={triggerAndroidDownload}
              className="btn-apple-primary"
              id="sticky-download-btn"
              style={{ padding: '7px 16px', fontSize: '0.78rem' }}
            >
              <Download size={13} />
              <span>Download for Android</span>
            </button>
          </div>
        </aside>
      )}

      {/* Friendly Download Toast Notification */}
      {downloadToast && (
        <div style={{
          position: 'fixed',
          top: '84px',
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
              Downloading Foundly for Android
            </div>
            <div style={{ fontSize: '0.74rem', color: 'var(--text-secondary)' }}>
              Open the downloaded file on your phone to install.
            </div>
          </div>
        </div>
      )}

      {/* QR Code and Installation Modal */}
      <DownloadModal 
        isOpen={isDownloadOpen} 
        onClose={() => setIsDownloadOpen(false)} 
        onDownloadAndroid={triggerAndroidDownload}
      />
    </div>
  );
}
