import React from 'react';
import { ArrowDown, Smartphone, QrCode, Download, ShieldCheck, Sparkles } from 'lucide-react';

export default function Hero({ onOpenDownload, onOpenQR }) {
  const scrollToAbout = () => {
    const el = document.getElementById('about');
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' });
    }
  };

  return (
    <section className="hero-wrapper" id="hero-section">
      {/* Background imagery */}
      <img 
        src="/assets/hero_sky.jpg" 
        alt="Freedom and relief of reconnecting lost belongings" 
        className="hero-bg-media"
      />
      <div className="hero-overlay-gradient"></div>

      {/* Top Hero Brand & Badge */}
      <div className="hero-content">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '12px' }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', background: 'rgba(0,0,0,0.3)', backdropFilter: 'blur(8px)', padding: '6px 14px', borderRadius: '999px', border: '1px solid rgba(255,255,255,0.2)' }}>
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#3DDC84', display: 'inline-block' }}></span>
            <span style={{ fontSize: '0.7rem', fontWeight: 700, letterSpacing: '0.14em', textTransform: 'uppercase' }}>
              Android Proximity Radar v2.4 Live
            </span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <span style={{ fontSize: '0.72rem', letterSpacing: '0.1em', textTransform: 'uppercase', opacity: 0.85 }}>
              4,820 items returned this month
            </span>
          </div>
        </div>

        {/* Huge Editorial Headline */}
        <div className="hero-title-wrap">
          <h1 className="text-editorial-title hero-main-title">
            LOST. SOUGHT. REUNITED.
          </h1>
        </div>

        {/* Dual Split Editorial Subtext - Matching Reference Style */}
        <div className="hero-editorial-subtext">
          <div className="subtext-col">
            Our platform is more than just a registry. It is a high-speed community network built to eliminate the despair of losing your essential belongings through instant proximity matching.
          </div>
          <div className="subtext-col">
            Experience real-time Bluetooth beacon radar, automated AI photo matching, and double-blind safe handoffs exclusively optimized for our native Android application.
          </div>
        </div>
      </div>

      {/* Bottom Hero Bar with Android Quick Download & Scroll Indicator */}
      <div className="hero-bottom-bar">
        {/* Android Quick Download Card */}
        <div className="hero-download-badge">
          <div className="hero-badge-icon">
            <Smartphone size={24} />
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span style={{ fontSize: '0.82rem', fontWeight: 700, letterSpacing: '0.04em' }}>FOUNDIT for Android</span>
              <span style={{ background: '#3DDC84', color: '#092013', fontSize: '0.62rem', fontWeight: 800, padding: '1px 6px', borderRadius: '3px' }}>APK / PLAY</span>
            </div>
            <p style={{ fontSize: '0.72rem', color: 'rgba(255, 255, 255, 0.75)', marginTop: '2px' }}>
              v2.4.2 · 18.4 MB · Free Download
            </p>
          </div>
          <div style={{ display: 'flex', gap: '8px' }}>
            <button 
              onClick={onOpenDownload}
              className="btn-terracotta"
              id="hero-download-apk-btn"
              style={{ padding: '8px 14px', fontSize: '0.72rem' }}
              title="Download Android APK"
            >
              <Download size={14} />
              <span>Install</span>
            </button>
            <button 
              onClick={onOpenQR}
              className="btn-pill-white"
              id="hero-scan-qr-btn"
              style={{ padding: '8px 12px', color: '#111' }}
              title="Scan QR Code with Phone"
            >
              <QrCode size={14} />
            </button>
          </div>
        </div>

        {/* Scroll Down Pill Indicator - Matching Reference Design */}
        <div style={{ textAlign: 'center', width: '100%', maxWidth: '200px', margin: '0 auto' }}>
          <button 
            onClick={scrollToAbout}
            className="btn-pill-white"
            id="hero-scroll-btn"
            style={{ 
              backgroundColor: 'rgba(255, 255, 255, 0.9)', 
              color: '#14171A', 
              boxShadow: '0 4px 15px rgba(0,0,0,0.15)',
              margin: '0 auto'
            }}
          >
            <span>SCROLL DOWN</span>
            <ArrowDown size={14} />
          </button>
        </div>

        {/* Right side verified stats */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', background: 'rgba(0,0,0,0.3)', backdropFilter: 'blur(10px)', padding: '10px 18px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.15)' }}>
          <ShieldCheck size={20} color="#3DDC84" />
          <div style={{ textAlign: 'left' }}>
            <div style={{ fontSize: '0.75rem', fontWeight: 700 }}>Encrypted & Safe</div>
            <div style={{ fontSize: '0.65rem', color: 'rgba(255,255,255,0.7)' }}>Zero Personal Data Leaked</div>
          </div>
        </div>
      </div>
    </section>
  );
}
