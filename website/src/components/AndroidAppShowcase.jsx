import React, { useState } from 'react';
import { 
  Smartphone, 
  Download, 
  QrCode, 
  ShieldCheck, 
  Radio, 
  Cpu, 
  MapPin, 
  CheckCircle2, 
  FileCheck,
  AlertCircle,
  ExternalLink
} from 'lucide-react';

export default function AndroidAppShowcase({ onOpenQR }) {
  const [downloadProgress, setDownloadProgress] = useState(0);
  const [isDownloading, setIsDownloading] = useState(false);
  const [downloadComplete, setDownloadComplete] = useState(false);
  const [activeTab, setActiveTab] = useState('features');

  const startApkDownload = () => {
    window.location.href = "https://download1085.mediafire.com/n5a6lagfhntgl5jcQFwPnLKT1a_S7BSkRa-zuDeFN2UHqLqbn5W20PE05JIStP51SkI0RjOCCnbYqo3L2DOibWcQ2WPmPOwuCk4xl3FTrTNlEJ1HAulg75rmEWWKlowA6oNh7zOdFJAcIzCCTb3IEFFgIUE9xVkSDlJoAvDOMyYXJg/1aeww8y7i8qaja8/foundly.apk";
  };

  return (
    <section className="section-android-showcase" id="android">
      <div className="android-editorial-grid">
        {/* Left Side: Android Phone Mockup with Radar Pulse */}
        <div className="phone-mockup-wrapper">
          <img 
            src="/assets/android_app.jpg" 
            alt="FOUNDIT Android App on Flagship Device" 
            className="phone-mockup-img"
          />

          {/* Floating Radar Detection Tag */}
          <div className="radar-floating-indicator">
            <div className="indicator-pulse-dot"></div>
            <div>
              <span style={{ color: '#38bdf8' }}>FOUND nearby:</span> Black Leather Wallet (250m)
            </div>
          </div>
        </div>

        {/* Right Side: Android App Details & Direct Download Engine */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '28px' }}>
          <div>
            <div className="badge-android-native">
              <Smartphone size={14} />
              <span>Native Android Application · v2.4.2</span>
            </div>
            <h2 className="text-section-title" style={{ color: '#FFFFFF', marginTop: '16px', fontSize: 'clamp(2.2rem, 4.5vw, 3.8rem)' }}>
              POWER IN YOUR POCKET.
            </h2>
            <p style={{ color: 'rgba(255, 255, 255, 0.75)', fontSize: '0.92rem', lineHeight: '1.7', marginTop: '12px' }}>
              Web registries are passive. Our Android client turns every community member’s device into an encrypted, low-power search radar. Designed with Material You aesthetics and privacy-by-design architecture.
            </p>
          </div>

          {/* Android Download Card */}
          <div className="android-download-card">
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '10px' }}>
              <div>
                <div style={{ fontSize: '1.1rem', fontWeight: 700, color: '#FFFFFF' }}>
                  Download FOUNDIT for Android
                </div>
                <div style={{ fontSize: '0.74rem', color: 'rgba(255,255,255,0.6)', marginTop: '2px' }}>
                  Release v2.4.2 · Updated Today · 18.4 MB
                </div>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', background: 'rgba(255,255,255,0.08)', padding: '4px 10px', borderRadius: '4px' }}>
                <span style={{ color: '#FBBF24', fontSize: '0.8rem' }}>★ 4.9</span>
                <span style={{ fontSize: '0.7rem', color: 'rgba(255,255,255,0.7)' }}>24k+ Downloads</span>
              </div>
            </div>

            {/* Download Progress Bar if Active */}
            {isDownloading && (
              <div style={{ width: '100%', background: 'rgba(255,255,255,0.1)', height: '8px', borderRadius: '4px', overflow: 'hidden' }}>
                <div 
                  style={{ 
                    width: `${downloadProgress}%`, 
                    height: '100%', 
                    background: '#3DDC84', 
                    transition: 'width 0.15s ease' 
                  }}
                />
              </div>
            )}

            {downloadComplete && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#3DDC84', fontSize: '0.78rem', fontWeight: 600 }}>
                <FileCheck size={18} />
                <span>APK Downloaded successfully! Open the file on your device to install.</span>
              </div>
            )}

            {/* Primary Download CTAs */}
            <div className="download-actions-grid">
              <button 
                onClick={startApkDownload}
                className="btn-apk-download"
                id="main-apk-download-btn"
                disabled={isDownloading}
              >
                <Download size={18} />
                <span>{isDownloading ? `Downloading (${downloadProgress}%)` : 'Direct APK Download'}</span>
              </button>

              <button 
                onClick={onOpenQR}
                className="btn-qr-scan"
                id="main-qr-modal-btn"
              >
                <QrCode size={18} />
                <span>Scan QR for Mobile</span>
              </button>
            </div>

            {/* Android System Compatibility Specs */}
            <div style={{ 
              display: 'grid', 
              gridTemplateColumns: 'repeat(3, 1fr)', 
              gap: '12px',
              borderTop: '1px solid rgba(255,255,255,0.1)',
              paddingTop: '16px',
              fontSize: '0.72rem',
              color: 'rgba(255,255,255,0.65)'
            }}>
              <div>
                <span style={{ display: 'block', color: '#FFFFFF', fontWeight: 600 }}>Requirement</span>
                Android 8.0+
              </div>
              <div>
                <span style={{ display: 'block', color: '#FFFFFF', fontWeight: 600 }}>Architecture</span>
                Universal ARM64 / v7a
              </div>
              <div>
                <span style={{ display: 'block', color: '#FFFFFF', fontWeight: 600 }}>Integrity</span>
                SHA-256 Verified
              </div>
            </div>
          </div>

          {/* Android-Only Feature Highlights */}
          <div className="app-feature-list">
            <div className="app-feature-item">
              <div className="app-feature-icon">
                <Radio size={16} />
              </div>
              <div>
                <h4 style={{ fontSize: '0.86rem', fontWeight: 600, color: '#FFFFFF' }}>Background BLE Proximity Radar</h4>
                <p style={{ fontSize: '0.78rem', color: 'rgba(255, 255, 255, 0.65)', marginTop: '2px' }}>
                  Silently scans for lost Bluetooth tags, keys, and registered wearables without draining battery (&lt; 0.8% daily).
                </p>
              </div>
            </div>

            <div className="app-feature-item">
              <div className="app-feature-icon">
                <Cpu size={16} />
              </div>
              <div>
                <h4 style={{ fontSize: '0.86rem', fontWeight: 600, color: '#FFFFFF' }}>AI Neural Photo Matcher</h4>
                <p style={{ fontSize: '0.78rem', color: 'rgba(255, 255, 255, 0.65)', marginTop: '2px' }}>
                  Snap an item with your camera; on-device neural model extracts engravings, scratches, and dimensions in milliseconds.
                </p>
              </div>
            </div>

            <div className="app-feature-item">
              <div className="app-feature-icon">
                <MapPin size={16} />
              </div>
              <div>
                <h4 style={{ fontSize: '0.86rem', fontWeight: 600, color: '#FFFFFF' }}>Safe Handoff Zones</h4>
                <p style={{ fontSize: '0.78rem', color: 'rgba(255, 255, 255, 0.65)', marginTop: '2px' }}>
                  Integrates with campus desks and certified 24/7 locations for contactless handoffs without personal meetings.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
