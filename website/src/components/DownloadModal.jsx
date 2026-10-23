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
              <svg width="86" height="86" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect x="5" y="5" width="28" height="28" rx="4" fill="#1E1510" />
                <rect x="9" y="9" width="20" height="20" rx="2" fill="#FFFFFF" />
                <rect x="13" y="13" width="12" height="12" fill="#8C5E3C" />

                <rect x="67" y="5" width="28" height="28" rx="4" fill="#1E1510" />
                <rect x="71" y="9" width="20" height="20" rx="2" fill="#FFFFFF" />
                <rect x="75" y="13" width="12" height="12" fill="#8C5E3C" />

                <rect x="5" y="67" width="28" height="28" rx="4" fill="#1E1510" />
                <rect x="9" y="71" width="20" height="20" rx="2" fill="#FFFFFF" />
                <rect x="13" y="75" width="12" height="12" fill="#8C5E3C" />

                <rect x="38" y="10" width="8" height="8" fill="#1E1510" />
                <rect x="50" y="10" width="10" height="8" fill="#8C5E3C" />
                <rect x="38" y="24" width="22" height="8" fill="#1E1510" />
                
                <rect x="10" y="38" width="8" height="16" fill="#1E1510" />
                <rect x="24" y="42" width="12" height="8" fill="#B69372" />
                <rect x="42" y="38" width="16" height="16" rx="3" fill="#8C5E3C" />
                <rect x="64" y="38" width="8" height="8" fill="#1E1510" />
                <rect x="78" y="44" width="16" height="10" fill="#1E1510" />

                <rect x="38" y="67" width="10" height="8" fill="#1E1510" />
                <rect x="52" y="67" width="14" height="18" fill="#1E1510" />
                <rect x="72" y="67" width="22" height="8" fill="#8C5E3C" />
                <rect x="72" y="81" width="10" height="12" fill="#1E1510" />
                <rect x="38" y="81" width="8" height="12" fill="#1E1510" />
              </svg>
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
