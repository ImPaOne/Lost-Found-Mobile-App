import React, { useState, useEffect } from 'react';
import { Smartphone, Download, QrCode, ShieldCheck, Check, Copy, HelpCircle } from 'lucide-react';

export default function DownloadModal({ isOpen, onClose, onDownloadApk }) {
  const [copied, setCopied] = useState(false);
  const [downloadStep, setDownloadStep] = useState('ready');

  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleDownload = () => {
    setDownloadStep('downloading');
    onDownloadApk();
    setTimeout(() => {
      setDownloadStep('done');
    }, 1000);
  };

  const copyChecksum = () => {
    navigator.clipboard.writeText('9b2d8e4f1a5c68430b1e564d7c80ab3e');
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="apple-modal-overlay" onClick={onClose} id="download-modal-backdrop">
      <div className="apple-modal-card" onClick={(e) => e.stopPropagation()}>
        <div style={{ padding: '24px 28px', borderBottom: '1px solid var(--border-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div style={{ width: '40px', height: '40px', borderRadius: '12px', background: '#F0F6FF', color: 'var(--apple-blue)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Smartphone size={22} />
            </div>
            <div>
              <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                Download for Android
              </h3>
              <p style={{ fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
                FOUNDIT Client · Release v2.4.2 (Stable)
              </p>
            </div>
          </div>
          <button 
            onClick={onClose}
            style={{ background: 'none', border: 'none', fontSize: '1.2rem', color: 'var(--text-tertiary)', cursor: 'pointer', padding: '4px' }}
          >
            ✕
          </button>
        </div>

        <div style={{ padding: '28px', maxHeight: '75vh', overflowY: 'auto' }}>
          {/* QR Code Scan Frame */}
          <div style={{ 
            display: 'flex', 
            alignItems: 'center', 
            gap: '20px', 
            background: '#F5F5F7', 
            padding: '20px', 
            borderRadius: '16px',
            marginBottom: '24px'
          }}>
            <div style={{ 
              background: '#FFFFFF', 
              padding: '10px', 
              borderRadius: '12px', 
              boxShadow: '0 2px 10px rgba(0,0,0,0.06)',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center'
            }}>
              <svg width="90" height="90" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect x="5" y="5" width="28" height="28" rx="4" fill="#1D1D1F" />
                <rect x="9" y="9" width="20" height="20" rx="2" fill="#FFFFFF" />
                <rect x="13" y="13" width="12" height="12" fill="#0071E3" />

                <rect x="67" y="5" width="28" height="28" rx="4" fill="#1D1D1F" />
                <rect x="71" y="9" width="20" height="20" rx="2" fill="#FFFFFF" />
                <rect x="75" y="13" width="12" height="12" fill="#0071E3" />

                <rect x="5" y="67" width="28" height="28" rx="4" fill="#1D1D1F" />
                <rect x="9" y="71" width="20" height="20" rx="2" fill="#FFFFFF" />
                <rect x="13" y="75" width="12" height="12" fill="#0071E3" />

                <rect x="38" y="10" width="8" height="8" fill="#1D1D1F" />
                <rect x="50" y="10" width="10" height="8" fill="#0071E3" />
                <rect x="38" y="24" width="22" height="8" fill="#1D1D1F" />
                
                <rect x="10" y="38" width="8" height="16" fill="#1D1D1F" />
                <rect x="24" y="42" width="12" height="8" fill="#279A67" />
                <rect x="42" y="38" width="16" height="16" rx="3" fill="#0071E3" />
                <rect x="64" y="38" width="8" height="8" fill="#1D1D1F" />
                <rect x="78" y="44" width="16" height="10" fill="#1D1D1F" />

                <rect x="38" y="67" width="10" height="8" fill="#1D1D1F" />
                <rect x="52" y="67" width="14" height="18" fill="#1D1D1F" />
                <rect x="72" y="67" width="22" height="8" fill="#0071E3" />
                <rect x="72" y="81" width="10" height="12" fill="#1D1D1F" />
                <rect x="38" y="81" width="8" height="12" fill="#279A67" />
              </svg>
              <span style={{ fontSize: '0.62rem', fontWeight: 700, color: 'var(--text-tertiary)', marginTop: '4px' }}>
                CAMERA SCAN
              </span>
            </div>

            <div style={{ flex: 1 }}>
              <div style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                Scan to Open on Android
              </div>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: '4px', lineHeight: '1.4' }}>
                Open your device's Camera or Google Lens to immediately begin downloading the application.
              </p>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginTop: '8px', fontSize: '0.74rem', color: '#279A67', fontWeight: 600 }}>
                <ShieldCheck size={14} />
                <span>Verified Direct APK Package</span>
              </div>
            </div>
          </div>

          {/* Action button */}
          <div style={{ marginBottom: '24px' }}>
            <button 
              onClick={handleDownload}
              className="btn-apple-primary"
              id="modal-download-trigger"
              style={{ width: '100%', padding: '14px', fontSize: '0.9rem', borderRadius: '12px' }}
            >
              <Download size={18} />
              <span>
                {downloadStep === 'done' ? '✓ APK Saved! Click to Download Again' : 'Download Direct APK (18.4 MB)'}
              </span>
            </button>
          </div>

          {/* Installation steps */}
          <div style={{ borderTop: '1px solid var(--border-subtle)', paddingTop: '18px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '10px' }}>
              <HelpCircle size={16} color="var(--apple-blue)" />
              <h4 style={{ fontSize: '0.84rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                How to install on Android:
              </h4>
            </div>

            <ol style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', paddingLeft: '20px', lineHeight: '1.6', display: 'flex', flexDirection: 'column', gap: '6px' }}>
              <li>Tap the <strong>Download Direct APK</strong> button to save <code style={{ background: '#ECECEE', padding: '1px 5px', borderRadius: '4px' }}>FOUNDIT-v2.4.2.apk</code>.</li>
              <li>When prompted in your Android browser, tap <strong>Open</strong> or tap the file in your notification bar.</li>
              <li>If asked, enable <em>"Allow from this source"</em> in Android Security Settings.</li>
              <li>Tap <strong>Install</strong> to launch FOUNDIT Lost &amp; Found.</li>
            </ol>
          </div>

          {/* Checksum */}
          <div style={{ 
            marginTop: '20px', 
            background: '#F5F5F7', 
            padding: '12px 16px', 
            borderRadius: '8px',
            fontSize: '0.74rem',
            color: 'var(--text-secondary)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between'
          }}>
            <div>
              <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>SHA-256: </span>
              <code>9b2d8e4f1a...7c80ab3e</code>
            </div>
            <button 
              onClick={copyChecksum}
              style={{ background: 'none', border: 'none', color: 'var(--apple-blue)', fontWeight: 600, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px' }}
            >
              {copied ? <Check size={13} /> : <Copy size={13} />}
              <span>{copied ? 'Copied' : 'Copy'}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
