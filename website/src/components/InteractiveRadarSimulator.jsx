import React, { useState } from 'react';
import { Radio, Volume2, Smartphone, Download, Check, Sparkles, Navigation } from 'lucide-react';

export default function InteractiveRadarSimulator({ onDownloadApk }) {
  const [distance, setDistance] = useState(12);
  const [selectedPreset, setSelectedPreset] = useState('keys');
  const [isAlerting, setIsAlerting] = useState(false);

  const presets = {
    keys: { name: 'My Keychain', dist: 12, loc: 'Library 2nd Floor Lounge' },
    wallet: { name: 'Fossil Leather Wallet', dist: 3, loc: 'Under Conference Chair' },
    bag: { name: 'Canvas Laptop Bag', dist: 24, loc: 'Campus Transit Stop' }
  };

  const handlePreset = (key) => {
    setSelectedPreset(key);
    setDistance(presets[key].dist);
  };

  const triggerBeep = () => {
    setIsAlerting(true);
    try {
      const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(880, audioCtx.currentTime);
      gain.gain.setValueAtTime(0.15, audioCtx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.3);
      osc.connect(gain);
      gain.connect(audioCtx.destination);
      osc.start();
      osc.stop(audioCtx.currentTime + 0.3);
    } catch (e) {
      // AudioContext fallback
    }
    setTimeout(() => setIsAlerting(false), 800);
  };

  // Calculate coordinates on the 260px dial
  // Center is (130, 130). Max radius is 110px representing 30m.
  const radius = Math.min(105, (distance / 30) * 105);
  // Angle at ~45 degrees
  const angle = (45 * Math.PI) / 180;
  const targetX = 130 + radius * Math.cos(angle) - 8;
  const targetY = 130 - radius * Math.sin(angle) - 8;

  const getSignalStatus = () => {
    if (distance <= 5) return { text: 'Immediate Proximity (< 5m)', color: '#279A67', strength: '98%' };
    if (distance <= 15) return { text: 'Strong Signal (Directional)', color: '#0071E3', strength: '78%' };
    return { text: 'Detecting Signal (Broad)', color: '#D97706', strength: '42%' };
  };

  const status = getSignalStatus();

  return (
    <section className="section-bento" style={{ background: '#FFFFFF' }} id="radar-demo">
      <div className="container-apple">
        <div className="bento-header" style={{ marginBottom: '32px' }}>
          <div style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--apple-blue)', letterSpacing: '0.08em', textTransform: 'uppercase', marginBottom: '8px' }}>
            INTERACTIVE DEMONSTRATION
          </div>
          <h2 className="bento-title">
            Test the Android BLE Radar.
          </h2>
          <p className="bento-desc">
            Experience how the native FOUNDIT Android radar pinpoints missing items in real-time. Slide the distance or choose a scenario below.
          </p>
        </div>

        {/* Interactive Box */}
        <div className="radar-simulation-box">
          {/* Left: Clean Light Radar Canvas */}
          <div style={{ textAlign: 'center' }}>
            <div className="radar-dial-canvas">
              {/* Outer 30m ring */}
              <div className="radar-dial-ring" style={{ width: '220px', height: '220px' }}></div>
              {/* Middle 15m ring */}
              <div className="radar-dial-ring" style={{ width: '130px', height: '130px' }}></div>
              {/* Inner 5m ring */}
              <div className="radar-dial-ring" style={{ width: '50px', height: '50px' }}></div>

              {/* Center point (You / Android Phone) */}
              <div className="radar-center-blip" title="Your Phone Location"></div>

              {/* Target Item blip */}
              <div 
                className="radar-target-blip"
                style={{ 
                  left: `${targetX}px`, 
                  top: `${targetY}px`,
                  transform: isAlerting ? 'scale(1.6)' : 'scale(1)',
                  boxShadow: isAlerting ? '0 0 25px #279A67' : '0 0 10px rgba(39, 154, 103, 0.6)'
                }}
                title={presets[selectedPreset].name}
              />
            </div>

            <div style={{ marginTop: '16px', fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
              Center: <strong>Your Android Device</strong> · Green Dot: <strong>{presets[selectedPreset].name}</strong>
            </div>
          </div>

          {/* Right: Controls & Metrics */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            {/* Presets */}
            <div>
              <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase', marginBottom: '8px' }}>
                Quick Test Scenarios:
              </label>
              <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                {Object.keys(presets).map((key) => (
                  <button
                    key={key}
                    onClick={() => handlePreset(key)}
                    style={{
                      padding: '8px 14px',
                      borderRadius: 'var(--radius-pill)',
                      border: selectedPreset === key ? '1px solid var(--apple-blue)' : '1px solid var(--border-subtle)',
                      background: selectedPreset === key ? '#F0F6FF' : '#FFFFFF',
                      color: selectedPreset === key ? 'var(--apple-blue)' : 'var(--text-secondary)',
                      fontSize: '0.8rem',
                      fontWeight: 600,
                      cursor: 'pointer',
                      transition: 'all 0.2s'
                    }}
                  >
                    {presets[key].name} ({presets[key].dist}m)
                  </button>
                ))}
              </div>
            </div>

            {/* Slider */}
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                <span style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                  Simulated Distance:
                </span>
                <span style={{ fontSize: '1.1rem', fontWeight: 700, color: status.color }}>
                  {distance} meters
                </span>
              </div>
              <input 
                type="range" 
                min="1" 
                max="30" 
                value={distance} 
                onChange={(e) => setDistance(Number(e.target.value))}
                style={{ width: '100%', accentColor: 'var(--apple-blue)', cursor: 'pointer' }}
                id="radar-distance-slider"
              />
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.72rem', color: 'var(--text-tertiary)', marginTop: '4px' }}>
                <span>1 meter (Immediate)</span>
                <span>15m</span>
                <span>30 meters (Range limit)</span>
              </div>
            </div>

            {/* Status Card */}
            <div style={{ background: '#F5F5F7', padding: '16px 20px', borderRadius: '12px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div>
                <div style={{ fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase' }}>
                  BLE Signal Precision
                </div>
                <div style={{ fontSize: '0.95rem', fontWeight: 700, color: status.color, marginTop: '2px' }}>
                  {status.text}
                </div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)', marginTop: '2px' }}>
                  Estimated RSSI: -{Math.round(45 + distance * 1.5)} dBm · Signal: {status.strength}
                </div>
              </div>

              <button 
                onClick={triggerBeep}
                className="btn-apple-secondary"
                style={{ padding: '8px 14px', fontSize: '0.78rem' }}
                title="Simulate acoustic finder pulse"
              >
                <Volume2 size={14} />
                <span>{isAlerting ? 'Pinging...' : 'Play Ping'}</span>
              </button>
            </div>

            {/* Action CTA */}
            <div>
              <button 
                onClick={onDownloadApk}
                className="btn-apple-primary"
                style={{ width: '100%', padding: '12px', fontSize: '0.88rem' }}
                id="radar-install-app-btn"
              >
                <Smartphone size={16} />
                <span>Install FOUNDIT on Android to Use Radar</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
