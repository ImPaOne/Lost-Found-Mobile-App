import React from 'react';
import { Download, QrCode, ShieldCheck, Smartphone, Check } from 'lucide-react';

export default function ModernEditorialHero({ onDownloadAndroid, onOpenQR }) {
  return (
    <section className="editorial-hero-fullscreen">
      <div className="container-fullscreen">
        {/* Top Micro Header */}
        <div className="hero-editorial-meta-row">
          <div className="hero-status-pill">
            <span className="pulsing-warm-dot"></span>
            <span>FOUNDLY FOR ANDROID</span>
          </div>

          <div className="hero-meta-stat">
            OVER 28,000 ITEMS REUNITED
          </div>
        </div>

        {/* Headline in simple, clean wording */}
        <div className="editorial-title-box">
          <h1 className="editorial-hero-title">
            LOST SOMETHING?<br />FIND IT WITH FOUNDLY.
          </h1>
        </div>

        {/* 3 Simple Columns in Plain English */}
        <div className="editorial-hero-split-text">
          <div className="hero-split-col">
            <span className="col-num-label">01 / QUICK PHOTO</span>
            <p className="col-paragraph">
              Found a wallet, keys, or a pet? Just snap a photo. Foundly matches it instantly with lost reports nearby.
            </p>
          </div>

          <div className="hero-split-col">
            <span className="col-num-label">02 / STAY PRIVATE</span>
            <p className="col-paragraph">
              Connect safely without giving away your personal phone number, email, or home address.
            </p>
          </div>

          <div className="hero-split-col hero-col-actions">
            <span className="col-num-label">03 / GET STARTED</span>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginTop: '4px' }}>
              <button 
                onClick={onDownloadAndroid}
                className="btn-apple-primary"
                id="hero-download-btn"
                style={{ width: '100%', padding: '12px 18px', fontSize: '0.84rem' }}
              >
                <Download size={15} />
                <span>Download for Android</span>
              </button>

              <button 
                onClick={onOpenQR}
                className="btn-apple-secondary"
                id="hero-scan-qr-btn"
                style={{ width: '100%', padding: '11px 18px', fontSize: '0.84rem' }}
              >
                <QrCode size={15} />
                <span>Scan QR Code</span>
              </button>
            </div>
          </div>
        </div>

        {/* Showcase Frame with Simple, Human Details */}
        <div className="hero-editorial-showcase">
          <div className="showcase-image-wrapper">
            <img 
              src="/assets/app_showcase_warm.jpg" 
              alt="Foundly App on Android Smartphone"
              className="showcase-hero-img" 
            />
          </div>

          <div className="showcase-info-side">
            <div className="showcase-badge-row">
              <span className="badge-warm-pill">
                <Smartphone size={13} />
                <span>Android App</span>
              </span>
              <span className="badge-warm-pill">
                <ShieldCheck size={13} />
                <span>100% Private</span>
              </span>
            </div>

            <h2 className="showcase-headline">
              Calm, fast recovery when it matters most.
            </h2>

            <p className="showcase-subtext">
              Foundly turns the stress of losing something into quick relief. No complicated menus, no ads, and no personal data sharing.
            </p>

            <div className="showcase-spec-list">
              <div className="showcase-spec-row">
                <span className="spec-row-label">Works on</span>
                <span className="spec-row-val">Samsung, Pixel, Xiaomi &amp; all Android phones</span>
              </div>
              <div className="showcase-spec-row">
                <span className="spec-row-label">System</span>
                <span className="spec-row-val">Android 8.0 or newer</span>
              </div>
              <div className="showcase-spec-row">
                <span className="spec-row-label">Cost</span>
                <span className="spec-row-val">Free to use</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
