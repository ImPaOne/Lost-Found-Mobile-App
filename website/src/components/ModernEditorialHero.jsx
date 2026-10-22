import React from 'react';
import { Smartphone, Download, QrCode, ArrowDown, ShieldCheck, Sparkles } from 'lucide-react';
import { playClickSound } from '../utils/audio';

export default function ModernEditorialHero({ onDownloadApk, onOpenQR }) {
  const scrollToSimulator = () => {
    playClickSound();
    const el = document.getElementById('interactive-app');
    if (el) el.scrollIntoView({ behavior: 'smooth' });
  };

  return (
    <section className="editorial-hero-fullscreen">
      <div className="container-fullscreen">
        {/* Top Micro Header */}
        <div className="hero-editorial-meta-row">
          <div className="hero-status-pill">
            <span className="pulsing-emerald-dot"></span>
            <span>FOUNDIT MOBILE ECOSYSTEM · ANDROID NATIVE RELEASE</span>
          </div>

          <div className="hero-meta-stat">
            28,490+ ITEMS REUNITED THIS SEMESTER
          </div>
        </div>

        {/* Giant Editorial Title Inspired by Reference Image 1 & 4 */}
        <div className="editorial-title-box">
          <h1 className="editorial-hero-title">
            RECONNECT WHAT'S LOST.
          </h1>
        </div>

        {/* Editorial Subtitle Columns (Matching Reference Image 1) */}
        <div className="editorial-hero-split-text">
          <div className="hero-split-col">
            <span className="col-num-label">01 / ARCHITECTURE</span>
            <p className="col-paragraph">
              A modern, decentralized mobile recovery network designed to turn frantic loss into quiet relief. Built with low-energy Bluetooth beacon triangulation and on-device neural vision.
            </p>
          </div>

          <div className="hero-split-col">
            <span className="col-num-label">02 / PROTOCOL</span>
            <p className="col-paragraph">
              Zero phone numbers or private emails disclosed. Claimants and finders connect through single-use cryptographic tokens and verified 24/7 campus safe-drop lockers.
            </p>
          </div>

          <div className="hero-split-col hero-col-actions">
            <span className="col-num-label">03 / GET STARTED</span>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginTop: '8px' }}>
              <button 
                onClick={onDownloadApk}
                className="btn-apple-primary"
                id="hero-apk-download"
                style={{ width: '100%', padding: '13px 20px', fontSize: '0.86rem' }}
              >
                <Download size={16} />
                <span>Download APK (v2.4.2)</span>
              </button>

              <button 
                onClick={scrollToSimulator}
                className="btn-apple-secondary"
                id="hero-test-simulator"
                style={{ width: '100%', padding: '12px 20px', fontSize: '0.86rem' }}
              >
                <Smartphone size={16} />
                <span>Try In-Browser App Simulator ↓</span>
              </button>
            </div>
          </div>
        </div>

        {/* Visual Banner Preview Image with Scroll Prompt */}
        <div className="hero-visual-banner" onClick={scrollToSimulator}>
          <img 
            src="/assets/hero_sky.jpg" 
            alt="Freedom of recovering lost items"
            className="hero-banner-photo" 
          />
          <div className="hero-banner-scrim"></div>
          
          <div className="hero-banner-inner-badge">
            <span style={{ fontSize: '0.75rem', fontWeight: 700, letterSpacing: '0.12em', textTransform: 'uppercase' }}>
              SCROLL DOWN TO TEST LIVE ANDROID APP
            </span>
            <ArrowDown size={14} />
          </div>
        </div>
      </div>
    </section>
  );
}
