import React, { useState } from 'react';
import { Search, MapPin, ArrowUpRight, CheckCircle2 } from 'lucide-react';
import { playClickSound } from '../utils/audio';

const registryItems = [
  {
    id: 1,
    title: 'Google Pixel 8 Pro (Obsidian)',
    category: 'tech',
    status: 'FOUND',
    badgeClass: 'status-badge-found',
    location: 'University Library · 3rd Floor',
    time: '25m ago',
    desc: 'Found on study desk in matte black protective casing.'
  },
  {
    id: 2,
    title: 'Vintage Leather Bifold & Keys',
    category: 'wallets',
    status: 'REUNITED',
    badgeClass: 'status-badge-reunited',
    location: 'Central Plaza Coffee Bar',
    time: '2h ago',
    desc: 'Safely claimed through single-use verification question.'
  },
  {
    id: 3,
    title: 'Golden Retriever Companion',
    category: 'pets',
    status: 'LOST',
    badgeClass: 'status-badge-lost',
    location: 'North Meadow Park Grounds',
    time: '45m ago',
    desc: 'Wearing reflective collar with engraved QR medallion.'
  },
  {
    id: 4,
    title: 'Sony WH-1000XM5 Case',
    category: 'tech',
    status: 'FOUND',
    badgeClass: 'status-badge-found',
    location: 'Transit Station · Terminal B',
    time: '3h ago',
    desc: 'Lodged safely at Station Information & Lost Property Desk.'
  }
];

export default function RecentItemsFeed({ onOpenQR }) {
  const [filter, setFilter] = useState('ALL');
  const [query, setQuery] = useState('');

  const filteredItems = registryItems.filter((item) => {
    const matchesFilter = filter === 'ALL' || item.status === filter;
    const matchesQuery = item.title.toLowerCase().includes(query.toLowerCase()) ||
                         item.location.toLowerCase().includes(query.toLowerCase());
    return matchesFilter && matchesQuery;
  });

  return (
    <section className="section-registry" id="directory">
      <div className="container-fullscreen">
        <div className="registry-header">
          <div>
            <div className="pillars-overline">COMMUNITY DIRECTORY</div>
            <h2 className="pillars-heading">
              Live Activity Registry.
            </h2>
          </div>
          <p style={{ maxWidth: '380px', fontSize: '0.86rem', color: 'var(--text-secondary)' }}>
            Real-time reports registered via the FOUNDIT Android client across local campus checkpoints.
          </p>
        </div>

        {/* Filter & Search Bar */}
        <div className="registry-filters-bar">
          <div className="filter-pills-row">
            {['ALL', 'FOUND', 'LOST', 'REUNITED'].map((tab) => (
              <button
                key={tab}
                onClick={() => {
                  playClickSound();
                  setFilter(tab);
                }}
                className={`filter-pill-btn ${filter === tab ? 'active' : ''}`}
              >
                {tab === 'ALL' ? 'All Items' : tab}
              </button>
            ))}
          </div>

          <div className="registry-search-box">
            <Search size={15} color="var(--brown-mocha)" />
            <input 
              type="text" 
              placeholder="Filter by item or location..."
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              className="registry-search-input"
            />
          </div>
        </div>

        {/* Minimal Item Cards */}
        <div className="registry-grid">
          {filteredItems.map((item) => (
            <div key={item.id} className="registry-card">
              <div>
                <div className="card-top-row">
                  <span className={item.badgeClass}>{item.status}</span>
                  <span className="item-time-text">{item.time}</span>
                </div>
                <div className="item-title-text">{item.title}</div>
                <div className="item-loc-text">
                  <MapPin size={13} color="var(--brown-caramel)" />
                  <span>{item.location}</span>
                </div>
                <div className="item-desc-text">{item.desc}</div>
              </div>

              <div className="item-footer-action">
                <span style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)', fontWeight: 600 }}>
                  Android Encrypted Log
                </span>
                <button onClick={onOpenQR} className="item-action-link">
                  <span>Verify</span>
                  <ArrowUpRight size={13} />
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
