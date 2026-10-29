import React, { useState } from 'react';
import { Smartphone, Download, QrCode, Radio, Camera, MapPin, Sparkles, CheckCircle2 } from 'lucide-react';

const APK_DOWNLOAD_URL = "https://download1085.mediafire.com/vzvk5smz39jgpbHU1sIWknTozxfuxUGwpMjCBqxvMiA7Anyc72sUr8fFMx2mcLWoV_9oOd7lKi6exXPRMfo5I0LhI4RJgG6fnVEzdBqjNhy1E5YIiKyRkJPM8ITC4jbeCPP0pyS-wM23zdDxKbv4REcaqKxE_4sznSjneJmeb0djuw/1aeww8y7i8qaja8/foundly.apk";

export default function AppleHero({ onDownloadApk, onOpenQR }) {
  const handleDownload = () => {
    if (onDownloadApk) {
      onDownloadApk();
    } else {
      window.location.href = APK_DOWNLOAD_URL;
    }
  };

  const [activeTab, setActiveTab] = useState('radar');

  const screens = {
    radar: {
      title: 'Proximity Radar',
      subtitle: 'Bluetooth BLE scanning up to 50 meters',
      image: '/assets/screen_radar.jpg',
      badge: '12m away · Strong Signal'
    },
    camera: {
      title: 'AI Visual Matcher',
      subtitle: 'Neural recognition of wallets, keys & tags',
      image: '/assets/screen_camera.jpg',
      badge: 'AI Detected: Leather Wallet (99% Match)'
    },
    device: {
      title: 'In-Hand Mobile View',
      subtitle: 'Native Material You interface for Android',
      image: '/assets/android_app.jpg',
      badge: 'Verified Finder Protocol Active'
    }
  };

  return (
    <section className="apple-hero" id="overview">
      <div className="container-apple">
        {/* Top Tag */}
        <div className="hero-pill-tag">
          <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#279A67', display: 'inline-block' }}></span>
          <span>FOUNDIT 2.4 for Android is here · Sub-meter BLE Radar</span>
        </div>

        {/* Main Grand Headlines */}
        <h1 className="apple-hero-headline">
          Never lose what matters.
        </h1>
        <p className="apple-hero-subheadline">
          The intelligent, community-powered Lost &amp; Found application for Android. Instant Bluetooth radar, on-device AI visual matching, and zero-knowledge private returns.
        </p>

        {/* Action CTAs */}
        <div className="hero-cta-group">
          <button 
            onClick={handleDownload}
            className="btn-apple-primary"
            id="hero-download-apk-action"
            style={{ padding: '14px 28px', fontSize: '0.95rem' }}
          >
            <Download size={18} />
            <span>Download Direct APK (18.4 MB)</span>
          </button>

          <button 
            onClick={onOpenQR}
            className="btn-apple-secondary"
            id="hero-scan-qr-action"
            style={{ padding: '14px 24px', fontSize: '0.95rem' }}
          >
            <QrCode size={18} />
            <span>Scan QR with Phone</span>
          </button>
        </div>

        {/* Interactive Device Screen Switcher */}
        <div className="device-showcase-frame">
          {/* Switcher Pill Tabs */}
          <div className="screen-switcher-tabs" role="tablist">
            <button 
              className={`screen-tab-btn ${activeTab === 'radar' ? 'active' : ''}`}
              onClick={() => setActiveTab('radar')}
              id="tab-radar"
            >
              <Radio size={14} />
              <span>Proximity Radar</span>
            </button>
            <button 
              className={`screen-tab-btn ${activeTab === 'camera' ? 'active' : ''}`}
              onClick={() => setActiveTab('camera')}
              id="tab-camera"
            >
              <Camera size={14} />
              <span>AI Camera Scan</span>
            </button>
            <button 
              className={`screen-tab-btn ${activeTab === 'device' ? 'active' : ''}`}
              onClick={() => setActiveTab('device')}
              id="tab-device"
            >
              <Smartphone size={14} />
              <span>Mobile In-Hand</span>
            </button>
          </div>

          {/* Clean Light Chassis with Screen */}
          <div style={{ position: 'relative', width: '100%', display: 'flex', justifyContent: 'center' }}>
            {/* Left Floating Badge */}
            <div className="device-feature-badge-left">
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#279A67', fontWeight: 700, fontSize: '0.72rem', textTransform: 'uppercase' }}>
                <CheckCircle2 size={15} />
                <span>Active Protection</span>
              </div>
              <div style={{ fontSize: '0.9rem', fontWeight: 700, color: '#1D1D1F', marginTop: '4px' }}>
                {screens[activeTab].badge}
              </div>
              <div style={{ fontSize: '0.75rem', color: '#86868B', marginTop: '2px' }}>
                Updated 2 seconds ago
              </div>
            </div>

            {/* Phone Screen Container */}
            <div className="phone-light-chassis">
              <div className="phone-screen-viewport">
                <img 
                  src={screens[activeTab].image} 
                  alt={screens[activeTab].title}
                  className="phone-screen-img"
                  key={activeTab}
                />
              </div>
            </div>

            {/* Right Floating Badge */}
            <div className="device-feature-badge-right">
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#0071E3', fontWeight: 700, fontSize: '0.72rem', textTransform: 'uppercase' }}>
                <Sparkles size={15} />
                <span>On-Device Neural</span>
              </div>
              <div style={{ fontSize: '0.9rem', fontWeight: 700, color: '#1D1D1F', marginTop: '4px' }}>
                {screens[activeTab].title}
              </div>
              <div style={{ fontSize: '0.75rem', color: '#86868B', marginTop: '2px' }}>
                {screens[activeTab].subtitle}
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
