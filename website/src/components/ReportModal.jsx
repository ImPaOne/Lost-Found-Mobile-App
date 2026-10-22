import React, { useState } from 'react';
import { Smartphone, Check, MapPin, Tag, Camera, AlertCircle } from 'lucide-react';

export default function ReportModal({ isOpen, onClose, onOpenDownload }) {
  const [type, setType] = useState('lost');
  const [category, setCategory] = useState('tech');
  const [title, setTitle] = useState('');
  const [location, setLocation] = useState('');
  const [submitted, setSubmitted] = useState(false);

  React.useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleSubmit = (e) => {
    e.preventDefault();
    setSubmitted(true);
  };

  const handleReset = () => {
    setSubmitted(false);
    setTitle('');
    setLocation('');
    onClose();
  };

  return (
    <div className="modal-overlay" onClick={onClose} id="report-modal-backdrop">
      <div className="modal-container" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div>
            <span className="text-overline" style={{ color: 'var(--terracotta)' }}>COMMUNITY REGISTRY</span>
            <h3 style={{ fontFamily: 'var(--font-serif)', fontSize: '1.45rem', fontWeight: 600, marginTop: '2px' }}>
              Report an Item
            </h3>
          </div>
          <button className="modal-close-btn" onClick={onClose}>✕</button>
        </div>

        <div className="modal-body">
          {submitted ? (
            <div style={{ textAlign: 'center', padding: '20px 10px' }}>
              <div style={{ width: '48px', height: '48px', borderRadius: '50%', background: '#279A67', color: '#FFF', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 16px' }}>
                <Check size={26} />
              </div>
              <h4 style={{ fontFamily: 'var(--font-serif)', fontSize: '1.6rem', color: 'var(--text-main)', marginBottom: '8px' }}>
                Item Registered Successfully!
              </h4>
              <p style={{ fontSize: '0.86rem', color: 'var(--text-muted)', lineHeight: '1.6', marginBottom: '20px' }}>
                Your report has been broadcast to the local web registry. For instant background radar matching and proximity push notifications, keep our Android app running.
              </p>
              <div style={{ display: 'flex', gap: '10px' }}>
                <button 
                  onClick={() => {
                    handleReset();
                    onOpenDownload();
                  }}
                  className="btn-terracotta"
                  style={{ flex: 1 }}
                >
                  <Smartphone size={15} />
                  <span>Download Android Radar</span>
                </button>
                <button 
                  onClick={handleReset}
                  className="btn-outline"
                >
                  Done
                </button>
              </div>
            </div>
          ) : (
            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {/* Type Switcher */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                <button
                  type="button"
                  onClick={() => setType('lost')}
                  style={{
                    padding: '10px',
                    borderRadius: '6px',
                    border: type === 'lost' ? '2px solid #E24A4A' : '1px solid var(--border-light)',
                    background: type === 'lost' ? 'rgba(226, 74, 74, 0.08)' : '#FFF',
                    fontWeight: 700,
                    fontSize: '0.78rem',
                    color: type === 'lost' ? '#E24A4A' : 'var(--text-muted)',
                    cursor: 'pointer'
                  }}
                >
                  I Lost An Item
                </button>
                <button
                  type="button"
                  onClick={() => setType('found')}
                  style={{
                    padding: '10px',
                    borderRadius: '6px',
                    border: type === 'found' ? '2px solid #279A67' : '1px solid var(--border-light)',
                    background: type === 'found' ? 'rgba(39, 154, 103, 0.08)' : '#FFF',
                    fontWeight: 700,
                    fontSize: '0.78rem',
                    color: type === 'found' ? '#279A67' : 'var(--text-muted)',
                    cursor: 'pointer'
                  }}
                >
                  I Found An Item
                </button>
              </div>

              {/* Category */}
              <div>
                <label style={{ display: 'block', fontSize: '0.72rem', fontWeight: 700, letterSpacing: '0.08em', textTransform: 'uppercase', marginBottom: '6px' }}>
                  Category
                </label>
                <select 
                  value={category} 
                  onChange={(e) => setCategory(e.target.value)}
                  style={{ width: '100%', padding: '10px 12px', borderRadius: '6px', border: '1px solid var(--border-light)', fontSize: '0.85rem' }}
                >
                  <option value="tech">Tech & Smartphones (IMEI / Serial)</option>
                  <option value="wallets">Wallets, Keys & IDs</option>
                  <option value="pets">Pets & Animals</option>
                  <option value="travel">Bags, Clothing & Luggage</option>
                </select>
              </div>

              {/* Title */}
              <div>
                <label style={{ display: 'block', fontSize: '0.72rem', fontWeight: 700, letterSpacing: '0.08em', textTransform: 'uppercase', marginBottom: '6px' }}>
                  Item Name & Description
                </label>
                <input 
                  type="text" 
                  placeholder="e.g. Black leather Fossil wallet with blue stripe"
                  required
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  style={{ width: '100%', padding: '10px 12px', borderRadius: '6px', border: '1px solid var(--border-light)', fontSize: '0.85rem' }}
                />
              </div>

              {/* Location */}
              <div>
                <label style={{ display: 'block', fontSize: '0.72rem', fontWeight: 700, letterSpacing: '0.08em', textTransform: 'uppercase', marginBottom: '6px' }}>
                  Approximate Location
                </label>
                <input 
                  type="text" 
                  placeholder="e.g. Science Library 2nd floor or Bus Line 14"
                  required
                  value={location}
                  onChange={(e) => setLocation(e.target.value)}
                  style={{ width: '100%', padding: '10px 12px', borderRadius: '6px', border: '1px solid var(--border-light)', fontSize: '0.85rem' }}
                />
              </div>

              {/* Android App callout banner inside report modal */}
              <div style={{ background: '#F8F6F1', border: '1px solid #E6E1D8', padding: '12px 14px', borderRadius: '6px', display: 'flex', gap: '10px', alignItems: 'center' }}>
                <Smartphone size={20} color="#3DDC84" style={{ flexShrink: 0 }} />
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', lineHeight: '1.4' }}>
                  <strong style={{ color: 'var(--text-main)' }}>Pro Tip:</strong> Using the <strong>Android App</strong> automatically attaches high-accuracy GPS coordinates & runs AI visual matching.
                </div>
              </div>

              <div style={{ display: 'flex', gap: '10px', marginTop: '6px' }}>
                <button type="submit" className="btn-terracotta" style={{ flex: 1 }}>
                  Submit Report
                </button>
                <button type="button" onClick={onClose} className="btn-outline">
                  Cancel
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
