import React, { useState } from 'react';
import { Copy, Check, ShieldCheck, Download, Smartphone } from 'lucide-react';

export default function TechSpecsSection({ onDownloadApk }) {
  const [copied, setCopied] = useState(false);

  const copyHash = () => {
    navigator.clipboard.writeText('9b2d8e4f1a5c68430b1e564d7c80ab3e');
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const specs = [
    { label: 'Compatible Android Versions', value: 'Android 8.0 (Oreo, API level 26) through Android 15' },
    { label: 'Package Format & Size', value: 'Direct APK Release · 18.4 MB (Universal ABI: arm64-v8a / armeabi-v7a)' },
    { label: 'Hardware Sensor Requirements', value: 'Bluetooth 5.0+ (BLE Central & Peripheral mode), GPS, Camera (optional for AI scanner)' },
    { label: 'Power Consumption', value: '< 0.8% battery usage per 24 hours (Android WorkManager batched polling)' },
    { label: 'Local Security & Privacy', value: 'Hardware-backed Android Keystore, AES-256 local encrypted cache, zero phone number storage' },
    { label: 'Developer Integrity Checksum', value: 'SHA-256: 9b2d8e4f1a5c68430b1e564d7c80ab3e' }
  ];

  return (
    <section className="section-tech-specs" id="specs">
      <div className="container-apple">
        <div style={{ maxWidth: '720px' }}>
          <div style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--apple-blue)', letterSpacing: '0.08em', textTransform: 'uppercase', marginBottom: '8px' }}>
            TECHNICAL SPECIFICATIONS
          </div>
          <h2 style={{ fontSize: 'clamp(2rem, 3.8vw, 3rem)', fontWeight: 800, letterSpacing: '-0.03em', color: 'var(--text-primary)' }}>
            Built for modern Android.
          </h2>
          <p style={{ fontSize: '1.1rem', color: 'var(--text-secondary)', marginTop: '10px' }}>
            Engineered with modern Kotlin, Jetpack Compose, and low-level NDK Bluetooth bindings for maximum performance and minimal battery overhead.
          </p>
        </div>

        {/* Specs Table */}
        <div className="specs-table">
          {specs.map((item, idx) => (
            <div key={idx} className="specs-row">
              <div className="specs-label">{item.label}</div>
              <div className="specs-value" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px' }}>
                <span>{item.value}</span>
                {idx === specs.length - 1 && (
                  <button 
                    onClick={copyHash}
                    style={{ background: 'none', border: 'none', color: 'var(--apple-blue)', fontWeight: 600, fontSize: '0.8rem', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px' }}
                  >
                    {copied ? <Check size={14} /> : <Copy size={14} />}
                    <span>{copied ? 'Copied' : 'Copy Hash'}</span>
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>

        {/* Bottom Banner */}
        <div style={{ 
          marginTop: '60px', 
          background: '#F5F5F7', 
          borderRadius: 'var(--radius-lg)', 
          padding: '40px', 
          display: 'flex', 
          alignItems: 'center', 
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '24px'
        }}>
          <div>
            <h3 style={{ fontSize: '1.4rem', fontWeight: 700, color: 'var(--text-primary)' }}>
              Ready to safeguard your belongings?
            </h3>
            <p style={{ fontSize: '0.92rem', color: 'var(--text-secondary)', marginTop: '4px' }}>
              Install FOUNDIT on your Android smartphone in less than 30 seconds.
            </p>
          </div>

          <button 
            onClick={onDownloadApk}
            className="btn-apple-primary"
            style={{ padding: '14px 28px', fontSize: '0.92rem' }}
          >
            <Download size={18} />
            <span>Download Direct APK (v2.4.2)</span>
          </button>
        </div>
      </div>
    </section>
  );
}
