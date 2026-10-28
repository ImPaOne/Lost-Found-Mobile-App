import React, { useState, useEffect } from 'react';
import { Smartphone, Download, ShieldCheck, HelpCircle } from 'lucide-react';

export default function DownloadModal({ isOpen, onClose, onDownloadAndroid }) {
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
    onDownloadAndroid();
    setTimeout(() => {
      setDownloadStep('done');
    }, 1000);
  };

  return (
    <div className="apple-modal-overlay" onClick={onClose} id="download-modal-backdrop">
      <div className="apple-modal-card" onClick={(e) => e.stopPropagation()}>
        <div style={{ padding: '22px 26px', borderBottom: '1px solid var(--border-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div style={{ width: '38px', height: '38px', borderRadius: '10px', background: 'var(--bg-subtle)', color: 'var(--brown-caramel)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Smartphone size={20} />
            </div>
            <div>
              <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: 'var(--text-primary)', fontFamily: 'var(--font-serif)' }}>
                Get Foundly for Android
              </h3>
              <p style={{ fontSize: '0.74rem', color: 'var(--text-secondary)' }}>
                Free download · Works on Android 8+
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

        <div style={{ padding: '24px', maxHeight: '75vh', overflowY: 'auto' }}>
          {/* QR Code Scan Frame */}
          <div style={{ 
            display: 'flex', 
            alignItems: 'center', 
            gap: '18px', 
            background: 'var(--bg-subtle)', 
            padding: '18px', 
            borderRadius: '14px',
            marginBottom: '20px',
            border: '1px solid var(--border-subtle)'
          }}>
            <div style={{ 
              background: '#FFFFFF', 
              padding: '10px', 
              borderRadius: '10px', 
              boxShadow: '0 2px 10px rgba(30,21,16,0.06)',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center'
            }}>
              <img 
                src="https://i.postimg.cc/GmQmXXNG/Untitled-(1).png" 
                alt="Scan QR Code to Download Foundly" 
                style={{ 
                  width: '110px', 
                  height: '110px', 
                  objectFit: 'contain', 
                  borderRadius: '6px',
                  display: 'block'
                }} 
              />
              <span style={{ fontSize: '0.62rem', fontWeight: 700, color: 'var(--text-tertiary)', marginTop: '4px' }}>
                CAMERA SCAN
              </span>
            </div>

            <div style={{ flex: 1 }}>
              <div style={{ fontSize: '0.92rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                Scan to install on phone
              </div>
              <p style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', marginTop: '4px', lineHeight: '1.4' }}>
                Open your phone's camera to download Foundly directly to your device.
              </p>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginTop: '8px', fontSize: '0.72rem', color: 'var(--brown-caramel)', fontWeight: 700 }}>
                <ShieldCheck size={14} />
                <span>Safe &amp; Verified Download</span>
              </div>
            </div>
          </div>

          {/* Action button */}
          <div style={{ marginBottom: '20px' }}>
            <button 
              onClick={handleDownload}
              className="btn-apple-primary"
              id="modal-download-trigger"
              style={{ width: '100%', padding: '13px', fontSize: '0.88rem', borderRadius: '12px' }}
            >
              <Download size={16} />
              <span>
                {downloadStep === 'done' ? '✓ Download Started! Tap to Download Again' : 'Download for Android'}
              </span>
            </button>
          </div>

          {/* Simple installation steps */}
          <div style={{ borderTop: '1px solid var(--border-subtle)', paddingTop: '16px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
              <HelpCircle size={15} color="var(--brown-caramel)" />
              <h4 style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                Quick install guide:
              </h4>
            </div>

            <ol style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', paddingLeft: '18px', lineHeight: '1.6', display: 'flex', flexDirection: 'column', gap: '4px' }}>
              <li>Tap <strong>Download for Android</strong> above.</li>
              <li>When the download finishes, tap the notification to open it.</li>
              <li>Tap <strong>Install</strong> to start using Foundly.</li>
            </ol>
          </div>
        </div>
      </div>
    </div>
  );
}
