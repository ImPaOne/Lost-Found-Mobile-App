import React from 'react';
import { Radio, Eye, Shield, QrCode, BatteryCharging, MapPin, Zap } from 'lucide-react';

export default function AppleBentoFeatures() {
  return (
    <section className="section-bento" id="features">
      <div className="container-apple">
        {/* Header */}
        <div className="bento-header">
          <div style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--apple-blue)', letterSpacing: '0.08em', textTransform: 'uppercase', marginBottom: '8px' }}>
            ENGINEERED FOR ANDROID
          </div>
          <h2 className="bento-title">
            Intelligent by design.<br />Effortless in action.
          </h2>
          <p className="bento-desc">
            FOUNDIT combines local Bluetooth hardware scanning, on-device machine vision, and privacy-first cryptography to give you instant peace of mind.
          </p>
        </div>

        {/* Bento Grid */}
        <div className="bento-grid">
          {/* Card 1: Sub-Meter Radar (Span 7) */}
          <div className="bento-card bento-card-span-7">
            <div>
              <div className="bento-icon-bubble">
                <Radio size={24} />
              </div>
              <h3 className="bento-card-title">Sub-Meter BLE Proximity Radar</h3>
              <p className="bento-card-p">
                Leverages your Android device’s Bluetooth Low Energy subsystem to continuously scan for registered tags, earbuds, and beacons within 50 meters. Visual concentric distance rings guide you straight to your item.
              </p>
            </div>
            <div style={{ marginTop: '24px', display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
              <span style={{ fontSize: '0.75rem', fontWeight: 600, background: '#F5F5F7', padding: '6px 12px', borderRadius: '6px' }}>
                Signal Strength (RSSI) Calibration
              </span>
              <span style={{ fontSize: '0.75rem', fontWeight: 600, background: '#F5F5F7', padding: '6px 12px', borderRadius: '6px' }}>
                Haptic Vibration Guidance
              </span>
            </div>
          </div>

          {/* Card 2: AI Neural Camera (Span 5) */}
          <div className="bento-card bento-card-span-5">
            <div>
              <div className="bento-icon-bubble" style={{ background: '#E8F5E9', color: '#279A67' }}>
                <Eye size={24} />
              </div>
              <h3 className="bento-card-title">On-Device AI Vision</h3>
              <p className="bento-card-p">
                Found a stray wallet or set of keys? Just point your camera. Our lightweight TensorFlow Lite model categorizes material, brand, and engravings in 180ms without uploading photos to the cloud.
              </p>
            </div>
            <div style={{ marginTop: '20px', fontSize: '0.75rem', color: '#279A67', fontWeight: 700 }}>
              99.4% Object Recognition Accuracy
            </div>
          </div>

          {/* Card 3: Zero-Knowledge Privacy (Span 4) */}
          <div className="bento-card bento-card-span-4">
            <div>
              <div className="bento-icon-bubble" style={{ background: '#FDF2E9', color: '#D97706' }}>
                <Shield size={24} />
              </div>
              <h3 className="bento-card-title">Zero Phone Leaks</h3>
              <p className="bento-card-p">
                Claim belongings and coordinate handoffs using anonymous double-blind verification codes. Your personal phone number and home address remain 100% private.
              </p>
            </div>
            <div style={{ marginTop: '20px', fontSize: '0.75rem', color: '#D97706', fontWeight: 700 }}>
              End-to-End Encrypted Relay
            </div>
          </div>

          {/* Card 4: Smart QR Generator (Span 4) */}
          <div className="bento-card bento-card-span-4">
            <div>
              <div className="bento-icon-bubble" style={{ background: '#F3E8FF', color: '#9333EA' }}>
                <QrCode size={24} />
              </div>
              <h3 className="bento-card-title">Smart QR Tags</h3>
              <p className="bento-card-p">
                Generate custom printable QR tags directly in the app. Affix them to pet collars, keychains, water bottles, or luggage for one-tap finder contact.
              </p>
            </div>
            <div style={{ marginTop: '20px', fontSize: '0.75rem', color: '#9333EA', fontWeight: 700 }}>
              Compatible with Any Phone Camera
            </div>
          </div>

          {/* Card 5: Battery Optimization (Span 4) */}
          <div className="bento-card bento-card-span-4">
            <div>
              <div className="bento-icon-bubble" style={{ background: '#E0F2FE', color: '#0284C7' }}>
                <BatteryCharging size={24} />
              </div>
              <h3 className="bento-card-title">All-Day Battery</h3>
              <p className="bento-card-p">
                Intelligent Android WorkManager scheduling uses hardware sensor batching, consuming less than 0.8% battery life across 24 hours of active background scanning.
              </p>
            </div>
            <div style={{ marginTop: '20px', fontSize: '0.75rem', color: '#0284C7', fontWeight: 700 }}>
              &lt; 0.8% Daily Battery Draw
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
