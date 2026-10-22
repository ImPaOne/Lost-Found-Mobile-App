import React from 'react';
import { ArrowUpRight, CheckCircle2, Lock, Navigation } from 'lucide-react';

export default function AboutSection({ onOpenDownload }) {
  return (
    <section className="section-about" id="about">
      {/* Editorial Header matching Reference */}
      <div className="section-header-editorial">
        <h2 className="text-section-title">ABOUT</h2>
        <a 
          href="#android" 
          className="editorial-link-btn"
          id="about-view-more-link"
        >
          <span>HOW IT WORKS</span>
          <ArrowUpRight size={15} />
        </a>
      </div>

      {/* Asymmetrical Editorial Grid */}
      <div className="about-grid-editorial">
        {/* Left Side: Editorial Photography */}
        <div className="about-portrait-card">
          <img 
            src="/assets/about_reunion.jpg" 
            alt="Warm relief of recovering a lost treasured item" 
            className="about-portrait-img"
          />
          <div style={{ 
            position: 'absolute', 
            bottom: '20px', 
            left: '20px', 
            right: '20px',
            background: 'rgba(255, 255, 255, 0.92)',
            backdropFilter: 'blur(10px)',
            padding: '16px 20px',
            borderRadius: '6px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            boxShadow: 'var(--shadow-md)'
          }}>
            <div>
              <div style={{ fontSize: '0.72rem', fontWeight: 700, letterSpacing: '0.12em', textTransform: 'uppercase', color: 'var(--terracotta)' }}>
                Case File #8921 · Verified Return
              </div>
              <div style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-main)', marginTop: '2px' }}>
                Handcrafted Leather Journal & Keys
              </div>
            </div>
            <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
              Returned in 2h 15m
            </div>
          </div>
        </div>

        {/* Right Side: Editorial Philosophy & Metrics */}
        <div className="about-text-content">
          <blockquote className="about-lead-quote">
            “True relief isn't just knowing your lost item exists somewhere—it’s having a secure, immediate bridge to bring it home safely.”
          </blockquote>

          <p className="about-body-p">
            The FOUNDIT platform was architected to replace outdated bulletin boards and chaotic social posts with an intelligent, privacy-first recovery network. We empower finders and owners to connect safely through anonymous end-to-end encrypted routing.
          </p>

          <p className="about-body-p">
            Using our native <strong>Android mobile client</strong>, community members can broadcast real-time geofence beacons, snap a photo for instant AI feature extraction, and arrange contactless returns through university desks, transit hubs, and verified local businesses.
          </p>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', margin: '8px 0' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '0.85rem', color: 'var(--text-main)' }}>
              <CheckCircle2 size={18} color="var(--terracotta)" />
              <span><strong>Zero Phone Disclosure:</strong> Direct in-app masked relay</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '0.85rem', color: 'var(--text-main)' }}>
              <CheckCircle2 size={18} color="var(--terracotta)" />
              <span><strong>Proof-of-Ownership:</strong> Secure serial, photo & question challenge</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '0.85rem', color: 'var(--text-main)' }}>
              <CheckCircle2 size={18} color="var(--terracotta)" />
              <span><strong>Instant Proximity Alerts:</strong> Android background low-energy BLE scanner</span>
            </div>
          </div>

          {/* Key Metric Numbers */}
          <div className="about-stats-row">
            <div className="stat-item">
              <span className="stat-num">96.4%</span>
              <span className="stat-label">Return Success Rate</span>
            </div>
            <div className="stat-item">
              <span className="stat-num">18,200+</span>
              <span className="stat-label">Belongings Restored</span>
            </div>
            <div className="stat-item">
              <span className="stat-num">&lt; 3.8h</span>
              <span className="stat-label">Median Recovery Time</span>
            </div>
          </div>

          <div style={{ marginTop: '12px' }}>
            <button 
              onClick={onOpenDownload}
              className="btn-dark"
              id="about-download-cta"
            >
              Get Android Client (v2.4.2)
            </button>
          </div>
        </div>
      </div>
    </section>
  );
}
