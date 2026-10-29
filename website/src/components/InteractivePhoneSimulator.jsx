import React, { useState, useEffect } from 'react';
import { 
  Radio, 
  Camera, 
  Search, 
  ShieldCheck, 
  Volume2, 
  MapPin, 
  Navigation, 
  CheckCircle2, 
  Sparkles, 
  Download, 
  QrCode,
  Battery,
  Wifi,
  Smartphone,
  ChevronRight
} from 'lucide-react';
import { playPingSound, playClickSound, playSuccessSound } from '../utils/audio';

const APK_DOWNLOAD_URL = "https://download1085.mediafire.com/vzvk5smz39jgpbHU1sIWknTozxfuxUGwpMjCBqxvMiA7Anyc72sUr8fFMx2mcLWoV_9oOd7lKi6exXPRMfo5I0LhI4RJgG6fnVEzdBqjNhy1E5YIiKyRkJPM8ITC4jbeCPP0pyS-wM23zdDxKbv4REcaqKxE_4sznSjneJmeb0djuw/1aeww8y7i8qaja8/foundly.apk";

export default function InteractivePhoneSimulator({ onDownloadApk, onOpenQR }) {
  const handleDownload = () => {
    if (onDownloadApk) {
      onDownloadApk();
    } else {
      window.location.href = APK_DOWNLOAD_URL;
    }
  };

  const [activeTab, setActiveTab] = useState('radar'); // radar, camera, feed, pass
  const [currentTime, setCurrentTime] = useState('10:42');
  const [selectedRadarItem, setSelectedRadarItem] = useState({
    id: 1,
    name: 'Car Key Fob & Tag',
    dist: 7,
    rssi: -54,
    location: 'Nearby Lounge Table',
    category: 'keys'
  });
  const [isPinging, setIsPinging] = useState(false);
  const [isScanningAI, setIsScanningAI] = useState(false);
  const [aiDetected, setAiDetected] = useState(false);
  const [phoneSearchQuery, setPhoneSearchQuery] = useState('');

  // Update real-time clock in phone status bar
  useEffect(() => {
    const updateTime = () => {
      const now = new Date();
      setCurrentTime(now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }));
    };
    updateTime();
    const interval = setInterval(updateTime, 30000);
    return () => clearInterval(interval);
  }, []);

  const switchTab = (tab) => {
    playClickSound();
    setActiveTab(tab);
  };

  const handleRingTag = () => {
    setIsPinging(true);
    playPingSound(920);
    setTimeout(() => {
      playPingSound(1040);
      setIsPinging(false);
    }, 400);
  };

  const handleScanAI = () => {
    setIsScanningAI(true);
    setAiDetected(false);
    playClickSound();
    setTimeout(() => {
      setIsScanningAI(false);
      setAiDetected(true);
      playSuccessSound();
    }, 1200);
  };

  const radarItems = [
    { id: 1, name: 'Car Key Fob & Tag', dist: 7, rssi: -54, x: 58, y: 35, category: 'keys' },
    { id: 2, name: 'Leather Bifold Wallet', dist: 14, rssi: -68, x: 28, y: 65, category: 'wallet' },
    { id: 3, name: 'Sony Earbuds Case', dist: 22, rssi: -82, x: 74, y: 78, category: 'tech' }
  ];

  const communityItems = [
    { id: 101, title: 'Google Pixel 8 Pro', loc: 'Main Campus Library', status: 'FOUND', time: '15m ago', tag: 'Electronics' },
    { id: 102, title: 'Golden Retriever (Milo)', loc: 'North Meadow Park', status: 'LOST', time: '40m ago', tag: 'Pet' },
    { id: 103, title: 'Saddle Leather Wallet', loc: 'Cafe Terrace', status: 'REUNITED', time: '2h ago', tag: 'Identity' },
    { id: 104, title: 'Sony WH-1000XM5 Case', loc: 'Bus Line 14', status: 'FOUND', time: '3h ago', tag: 'Audio' }
  ].filter(i => i.title.toLowerCase().includes(phoneSearchQuery.toLowerCase()) || i.loc.toLowerCase().includes(phoneSearchQuery.toLowerCase()));

  return (
    <div className="phone-simulator-section" id="interactive-app">
      <div className="phone-sim-layout">
        {/* Left Side: Modern Interactive Controller & Copy */}
        <div className="phone-sim-copy-side">
          <div className="tag-pill-emerald">
            <span className="emerald-dot"></span>
            <span>Live Interactive Android Simulator</span>
          </div>

          <h2 className="editorial-headline-large">
            Experience the app.<br />
            Before you install.
          </h2>

          <p className="editorial-subhead-text">
            Test real Android features right on your screen. Tap between <strong>Radar</strong>, <strong>AI Scanner</strong>, and <strong>Community Feed</strong> to see how FOUNDIT locates missing items in seconds.
          </p>

          {/* Interactive Screen Selectors */}
          <div className="sim-feature-buttons-stack">
            <button 
              className={`sim-feature-btn ${activeTab === 'radar' ? 'active' : ''}`}
              onClick={() => switchTab('radar')}
            >
              <div className="sim-btn-icon-wrap" style={{ background: '#E0F2FE', color: '#0284C7' }}>
                <Radio size={18} />
              </div>
              <div style={{ textAlign: 'left', flex: 1 }}>
                <div style={{ fontWeight: 700, fontSize: '0.9rem', color: '#111827' }}>1. Bluetooth Proximity Radar</div>
                <div style={{ fontSize: '0.78rem', color: '#6B7280' }}>Continuously tracks signal strength (RSSI) within 50m</div>
              </div>
              <ChevronRight size={16} color="#9CA3AF" />
            </button>

            <button 
              className={`sim-feature-btn ${activeTab === 'camera' ? 'active' : ''}`}
              onClick={() => switchTab('camera')}
            >
              <div className="sim-btn-icon-wrap" style={{ background: '#ECFDF5', color: '#059669' }}>
                <Camera size={18} />
              </div>
              <div style={{ textAlign: 'left', flex: 1 }}>
                <div style={{ fontWeight: 700, fontSize: '0.9rem', color: '#111827' }}>2. AI Neural Camera Match</div>
                <div style={{ fontSize: '0.78rem', color: '#6B7280' }}>On-device object and material classification in 180ms</div>
              </div>
              <ChevronRight size={16} color="#9CA3AF" />
            </button>

            <button 
              className={`sim-feature-btn ${activeTab === 'feed' ? 'active' : ''}`}
              onClick={() => switchTab('feed')}
            >
              <div className="sim-btn-icon-wrap" style={{ background: '#FEF3C7', color: '#D97706' }}>
                <Search size={18} />
              </div>
              <div style={{ textAlign: 'left', flex: 1 }}>
                <div style={{ fontWeight: 700, fontSize: '0.9rem', color: '#111827' }}>3. Real-Time Community Feed</div>
                <div style={{ fontSize: '0.78rem', color: '#6B7280' }}>Browse nearby lost &amp; found items with instant search</div>
              </div>
              <ChevronRight size={16} color="#9CA3AF" />
            </button>

            <button 
              className={`sim-feature-btn ${activeTab === 'pass' ? 'active' : ''}`}
              onClick={() => switchTab('pass')}
            >
              <div className="sim-btn-icon-wrap" style={{ background: '#F5F3FF', color: '#7C3AED' }}>
                <ShieldCheck size={18} />
              </div>
              <div style={{ textAlign: 'left', flex: 1 }}>
                <div style={{ fontWeight: 700, fontSize: '0.9rem', color: '#111827' }}>4. Anonymous Handover Pass</div>
                <div style={{ fontSize: '0.78rem', color: '#6B7280' }}>Double-blind QR token verification at safe partner desks</div>
              </div>
              <ChevronRight size={16} color="#9CA3AF" />
            </button>
          </div>

          {/* Quick Install Action Button */}
          <div style={{ marginTop: '28px', display: 'flex', gap: '14px', flexWrap: 'wrap' }}>
            <button 
              onClick={handleDownload}
              className="btn-apple-primary"
              id="sim-direct-download-btn"
              style={{ padding: '14px 28px', fontSize: '0.92rem' }}
            >
              <Download size={18} />
              <span>Download Direct APK (18.4 MB)</span>
            </button>

            <button 
              onClick={onOpenQR}
              className="btn-apple-secondary"
              id="sim-scan-qr-btn"
              style={{ padding: '14px 22px', fontSize: '0.92rem' }}
            >
              <QrCode size={18} />
              <span>Scan QR Code</span>
            </button>
          </div>

          <div style={{ marginTop: '12px', fontSize: '0.75rem', color: '#6B7280' }}>
            Compatible with Android 8.0 through Android 15 · No Google Play required
          </div>
        </div>

        {/* Right Side: The Living Interactive Android Phone */}
        <div className="phone-sim-device-container">
          <div className="android-phone-frame">
            {/* Camera Punch Hole */}
            <div className="phone-punch-hole"></div>

            {/* Android Status Bar */}
            <div className="phone-status-bar">
              <span style={{ fontWeight: 600 }}>{currentTime}</span>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <span style={{ fontSize: '0.62rem', fontWeight: 700 }}>5G</span>
                <Wifi size={12} />
                <Battery size={13} />
              </div>
            </div>

            {/* In-App Content Header */}
            <div className="phone-app-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <div style={{ width: '22px', height: '22px', borderRadius: '6px', background: '#10B981', color: '#FFF', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.68rem', fontWeight: 800 }}>
                  F
                </div>
                <span style={{ fontWeight: 700, fontSize: '0.85rem', color: '#111827' }}>FOUNDIT</span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '0.65rem', background: '#ECFDF5', color: '#047857', padding: '2px 8px', borderRadius: '999px', fontWeight: 700 }}>
                <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: '#10B981' }}></span>
                <span>RADAR ON</span>
              </div>
            </div>

            {/* In-App Screen Views */}
            <div className="phone-screen-body">
              {/* SCREEN 1: RADAR */}
              {activeTab === 'radar' && (
                <div className="phone-screen-radar-view">
                  <div className="radar-canvas-wrap">
                    {/* Concentric distance rings */}
                    <div className="radar-ring-circle ring-30m"></div>
                    <div className="radar-ring-circle ring-20m"></div>
                    <div className="radar-ring-circle ring-10m"></div>
                    
                    {/* Radar Sweep line */}
                    <div className="radar-sweep-beam"></div>

                    {/* Center: Android Phone */}
                    <div className="radar-center-marker">
                      <div className="radar-center-core"></div>
                    </div>

                    {/* Interactive Radar Blips */}
                    {radarItems.map((item) => {
                      const isSelected = selectedRadarItem?.id === item.id;
                      return (
                        <div 
                          key={item.id}
                          className={`radar-item-blip ${isSelected ? 'active-blip' : ''}`}
                          style={{ left: `${item.x}%`, top: `${item.y}%` }}
                          onClick={() => {
                            playClickSound();
                            setSelectedRadarItem(item);
                          }}
                        >
                          <div className="radar-blip-pulse"></div>
                          <span className="radar-blip-label">{item.name.split(' ')[0]} ({item.dist}m)</span>
                        </div>
                      );
                    })}
                  </div>

                  {/* Selected Item Floating Details Card */}
                  {selectedRadarItem && (
                    <div className="radar-detail-dock">
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '6px' }}>
                        <div>
                          <div style={{ fontSize: '0.82rem', fontWeight: 700, color: '#111827' }}>
                            {selectedRadarItem.name}
                          </div>
                          <div style={{ fontSize: '0.7rem', color: '#047857', fontWeight: 600 }}>
                            {selectedRadarItem.dist}m away · Signal RSSI: {selectedRadarItem.rssi} dBm
                          </div>
                        </div>
                        <div style={{ fontSize: '0.65rem', background: '#F3F4F6', padding: '3px 7px', borderRadius: '4px', fontWeight: 600, color: '#4B5563' }}>
                          Tag #8942
                        </div>
                      </div>

                      <div style={{ display: 'flex', gap: '8px', marginTop: '10px' }}>
                        <button 
                          onClick={handleRingTag}
                          className="phone-action-btn-primary"
                          style={{ flex: 1 }}
                        >
                          <Volume2 size={13} />
                          <span>{isPinging ? 'Pinging Tag...' : 'Ring Tag'}</span>
                        </button>
                        <button 
                          onClick={() => alert(`Navigating to ${selectedRadarItem.name}: head 7 meters North toward Lounge.`)}
                          className="phone-action-btn-secondary"
                        >
                          <Navigation size={13} />
                          <span>Guide</span>
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              )}

              {/* SCREEN 2: AI CAMERA SCANNER */}
              {activeTab === 'camera' && (
                <div className="phone-screen-camera-view">
                  <div className="camera-viewfinder-overlay">
                    <img 
                      src="/assets/cat_wallet.jpg" 
                      alt="Item in View" 
                      style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                    />
                    
                    {/* Bounding Box Reticle */}
                    <div className={`camera-bounding-box ${isScanningAI ? 'scanning' : ''} ${aiDetected ? 'detected' : ''}`}>
                      <div className="corner-tl"></div>
                      <div className="corner-tr"></div>
                      <div className="corner-bl"></div>
                      <div className="corner-br"></div>
                      
                      {isScanningAI && <div className="scanning-laser-line"></div>}

                      {aiDetected && (
                        <div className="camera-match-pill">
                          <Sparkles size={11} color="#10B981" />
                          <span>99.2% Match: Saddle Leather Bifold</span>
                        </div>
                      )}
                    </div>

                    {/* Camera Control Footer */}
                    <div className="camera-controls-bar">
                      <button 
                        onClick={handleScanAI}
                        className="camera-shutter-btn"
                        title="Tap to scan item with AI"
                      >
                        <div className="shutter-inner"></div>
                      </button>
                      <div style={{ fontSize: '0.68rem', color: '#FFF', textShadow: '0 1px 4px rgba(0,0,0,0.6)', marginTop: '6px' }}>
                        {isScanningAI ? 'Extracting visual features...' : aiDetected ? 'Verified match found!' : 'Tap button to scan item'}
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {/* SCREEN 3: FEED */}
              {activeTab === 'feed' && (
                <div className="phone-screen-feed-view">
                  <div style={{ padding: '12px 14px 6px' }}>
                    <div className="phone-search-input-box">
                      <Search size={14} color="#9CA3AF" />
                      <input 
                        type="text"
                        placeholder="Search nearby items..."
                        value={phoneSearchQuery}
                        onChange={(e) => setPhoneSearchQuery(e.target.value)}
                        className="phone-search-field"
                      />
                    </div>
                  </div>

                  <div className="phone-feed-scroll">
                    {communityItems.map((item) => (
                      <div key={item.id} className="phone-feed-item-card" onClick={() => alert(`Details for ${item.title}: Reported at ${item.loc}. Use Android app to claim.`)}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <span className={`phone-status-badge status-${item.status.toLowerCase()}`}>
                            {item.status}
                          </span>
                          <span style={{ fontSize: '0.62rem', color: '#9CA3AF' }}>{item.time}</span>
                        </div>
                        <div style={{ fontSize: '0.8rem', fontWeight: 700, color: '#111827', marginTop: '4px' }}>
                          {item.title}
                        </div>
                        <div style={{ fontSize: '0.68rem', color: '#6B7280', display: 'flex', alignItems: 'center', gap: '3px', marginTop: '2px' }}>
                          <MapPin size={10} color="#059669" />
                          <span>{item.loc}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* SCREEN 4: SAFE PASS */}
              {activeTab === 'pass' && (
                <div className="phone-screen-pass-view">
                  <div className="pass-card-sheet">
                    <div style={{ textAlign: 'center', marginBottom: '12px' }}>
                      <div style={{ fontSize: '0.68rem', fontWeight: 700, color: '#047857', letterSpacing: '0.08em', textTransform: 'uppercase' }}>
                        CRYPTOGRAPHIC CLAIM PASS
                      </div>
                      <div style={{ fontSize: '0.95rem', fontWeight: 800, color: '#111827', marginTop: '2px' }}>
                        Token #8492-FND
                      </div>
                    </div>

                    {/* QR Code in phone pass */}
                    <div style={{ background: '#FFF', padding: '12px', borderRadius: '10px', boxShadow: '0 2px 8px rgba(0,0,0,0.06)', margin: '0 auto', width: 'fit-content' }}>
                      <img 
                        src="https://i.postimg.cc/CKkS022d/qr.png" 
                        onError={(e) => { e.target.onerror = null; e.target.src = '/assets/qr.png'; }}
                        alt="Foundly QR Code"
                        style={{ width: '120px', height: '120px', objectFit: 'contain', display: 'block' }}
                      />
                    </div>

                    <div style={{ textAlign: 'center', marginTop: '12px' }}>
                      <div style={{ fontSize: '0.7rem', color: '#4B5563' }}>
                        Present at <strong>Campus Security 24/7 Desk</strong>
                      </div>
                      <div style={{ fontSize: '0.62rem', color: '#9CA3AF', marginTop: '2px' }}>
                        Single-use zero-knowledge claim PIN
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </div>

            {/* Android In-App Bottom Navigation Bar */}
            <div className="phone-bottom-nav">
              <button 
                className={`phone-nav-tab ${activeTab === 'radar' ? 'active' : ''}`}
                onClick={() => switchTab('radar')}
              >
                <Radio size={16} />
                <span>Radar</span>
              </button>

              <button 
                className={`phone-nav-tab ${activeTab === 'camera' ? 'active' : ''}`}
                onClick={() => switchTab('camera')}
              >
                <Camera size={16} />
                <span>AI Scan</span>
              </button>

              <button 
                className={`phone-nav-tab ${activeTab === 'feed' ? 'active' : ''}`}
                onClick={() => switchTab('feed')}
              >
                <Search size={16} />
                <span>Feed</span>
              </button>

              <button 
                className={`phone-nav-tab ${activeTab === 'pass' ? 'active' : ''}`}
                onClick={() => switchTab('pass')}
              >
                <ShieldCheck size={16} />
                <span>Pass</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
