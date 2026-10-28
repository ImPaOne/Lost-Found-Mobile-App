import React from 'react';
import { Camera, ShieldCheck, MapPin, ArrowUpRight } from 'lucide-react';

export default function CorePillars({ onDownloadAndroid }) {
  const pillars = [
    {
      num: '01',
      icon: <Camera size={22} />,
      title: 'Snap a Photo',
      desc: 'Take a quick photo of what you found. Foundly spots key details like color, type, and brand to find the rightful owner fast.',
      tag: 'Instant photo search'
    },
    {
      num: '02',
      icon: <ShieldCheck size={22} />,
      title: 'Keep Your Privacy',
      desc: 'You never have to share your phone number or home address. Simply answer a quick detail only the true owner would know.',
      tag: 'No phone number shared'
    },
    {
      num: '03',
      icon: <MapPin size={22} />,
      title: 'Safe Drop Spots',
      desc: 'Drop off or pick up items safely at campus security desks and 24/7 lockers. No meeting strangers in awkward places.',
      tag: 'Verified drop locations'
    }
  ];

  return (
    <section className="section-core-pillars" id="features">
      <div className="container-fullscreen">
        <div className="pillars-header">
          <div>
            <div className="pillars-overline">HOW IT WORKS</div>
            <h2 className="pillars-heading">
              Three simple steps to<br />
              get your items back.
            </h2>
          </div>
          <button 
            onClick={onDownloadAndroid}
            className="btn-apple-secondary"
            style={{ fontSize: '0.8rem', padding: '9px 18px' }}
          >
            <span>Download for Android</span>
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
