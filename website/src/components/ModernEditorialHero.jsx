import React from 'react';
import { Download, QrCode, Shield, CheckCircle2, Smartphone } from 'lucide-react';
import { playClickSound } from '../utils/audio';

export default function ModernEditorialHero({ onDownloadApk, onOpenQR }) {
  const scrollToPillars = () => {
    playClickSound();
    const el = document.getElementById('pillars');
    if (el) el.scrollIntoView({ behavior: 'smooth' });
  };

  return (
    <section className="editorial-hero-fullscreen">
      <div className="container-fullscreen">
        {/* Top Micro Header */}
        <div className="hero-editorial-meta-row">
          <div className="hero-status-pill">
            <span className="pulsing-warm-dot"></span>
            <span>FOUNDIT MOBILE · ANDROID NATIVE v2.4.2</span>
          </div>

          <div className="hero-meta-stat">
            28,490+ VERIFIED ITEMS REUNITED
          </div>
        </div>

        {/* Giant Editorial Headline */}
        <div className="editorial-title-box">
          <h1 className="editorial-hero-title">
            RECONNECT WHAT'S LOST.
          </h1>
        </div>

        {/* Editorial Subtitle Columns */}
        <div className="editorial-hero-split-text">
          <div className="hero-split-col">
            <span className="col-num-label">01 / PHILOSOPHY</span>
            <p className="col-paragraph">
              A minimalist, community-centered recovery network built exclusively for Android. Designed to turn stressful misplacement into swift, quiet relief without friction.
            </p>
          </div>

          <div className="hero-split-col">
            <span className="col-num-label">02 / PROTOCOL</span>
            <p className="col-paragraph">
              Zero contact disclosure. Finders and owners connect via single-use verification tokens and secure safe-drop points without exposing phone numbers or private emails.
            </p>
          </div>

          <div className="hero-split-col hero-col-actions">
            <span className="col-num-label">03 / INSTALLATION</span>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginTop: '4px' }}>
              <button 
                onClick={onDownloadApk}
                className="btn-apple-primary"
                id="hero-apk-download"
                style={{ width: '100%', padding: '12px 18px', fontSize: '0.84rem' }}
              >
                <Download size={15} />
                <span>Download APK (v2.4.2)</span>
              </button>

              <button 
                onClick={onOpenQR}
                className="btn-apple-secondary"
                id="hero-scan-qr-btn"
                style={{ width: '100%', padding: '11px 18px', fontSize: '0.84rem' }}
              >
                <QrCode size={15} />
                <span>Scan QR for Mobile</span>
              </button>
            </div>
          </div>
        </div>

        {/* High-End Editorial Showcase (Warm Minimalist Aesthetic) */}
        <div className="hero-editorial-showcase">
          <div className="showcase-image-wrapper">
            <img 
              src="/assets/app_showcase_warm.jpg" 
              alt="FOUNDIT Android Application on Flagship Device"
              className="showcase-hero-img" 
            />
          </div>

          <div className="showcase-info-side">
            <div className="showcase-badge-row">
              <span className="badge-warm-pill">
                <Smartphone size={13} />
                <span>Android 8.0 - 15</span>
              </span>
              <span className="badge-warm-pill">
                <Shield size={13} />
                <span>Zero Trackers</span>
              </span>
            </div>

            <h2 className="showcase-headline">
              Minimalist by intention. Effortless in practice.
            </h2>

            <p className="showcase-subtext">
              Built using native Android components and a warm editorial design language. No clutter, no repetitive feeds, and no invasive permissions. Point your camera, log what you found, or verify ownership in seconds.
            </p>

            <div className="showcase-spec-list">
              <div className="showcase-spec-row">
                <span className="spec-row-label">Application Package</span>
                <span className="spec-row-val">com.foundit.lostfound.android</span>
              </div>
              <div className="showcase-spec-row">
                <span className="spec-row-label">Target Architecture</span>
                <span className="spec-row-val">Universal ARM64-v8a</span>
              </div>
              <div className="showcase-spec-row">
                <span className="spec-row-label">Academic Scope</span>
                <span className="spec-row-val">HDSE 26.1 MAD Project</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
