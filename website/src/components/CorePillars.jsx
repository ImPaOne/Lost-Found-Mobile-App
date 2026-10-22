import React from 'react';
import { Camera, ShieldCheck, MapPin, ArrowUpRight } from 'lucide-react';
import { playClickSound } from '../utils/audio';

export default function CorePillars({ onDownloadApk }) {
  const pillars = [
    {
      num: '01',
      icon: <Camera size={22} />,
      title: 'On-Device AI Vision',
      desc: 'Point your camera at any stray wallet, keychain, or device. Lightweight neural classification identifies brands, leather grains, and serial prefixes in 180ms without sending photos to cloud servers.',
      tag: 'Local Neural Classification'
    },
    {
      num: '02',
      icon: <ShieldCheck size={22} />,
      title: 'Zero-Knowledge Privacy',
      desc: 'Owners confirm ownership through blind verification questions (card initials, distinctive markings, or lockscreen clues). Personal phone numbers and home addresses stay 100% private.',
      tag: 'Cryptographic Claim Verification'
    },
    {
      num: '03',
      icon: <MapPin size={22} />,
      title: 'Safe Campus Drop Hubs',
      desc: 'No awkward public meetups with strangers. Finders deposit verified items at university security desks and 24/7 automated lockers for secure, contactless collection.',
      tag: '350+ Verified Return Points'
    }
  ];

  return (
    <section className="section-core-pillars" id="pillars">
      <div className="container-fullscreen">
        <div className="pillars-header">
          <div>
            <div className="pillars-overline">SYSTEM ARCHITECTURE</div>
            <h2 className="pillars-heading">
              Thoughtful design.<br />
              Zero unnecessary noise.
            </h2>
          </div>
          <button 
            onClick={onDownloadApk}
            className="btn-apple-secondary"
            style={{ fontSize: '0.8rem', padding: '9px 18px' }}
          >
            <span>Get Android Client</span>
            <ArrowUpRight size={14} />
          </button>
        </div>

        <div className="pillars-grid">
          {pillars.map((p) => (
            <div key={p.num} className="pillar-card">
              <div>
                <div className="pillar-top-meta">
                  <span className="pillar-number">{p.num}</span>
                  <div className="pillar-icon-box">
                    {p.icon}
                  </div>
                </div>
                <h3 className="pillar-title">{p.title}</h3>
                <p className="pillar-desc">{p.desc}</p>
              </div>
              <div className="pillar-tag">{p.tag}</div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
