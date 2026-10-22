import React, { useState } from 'react';
import { Search, MapPin, Clock, ShieldCheck, ArrowUpRight, Filter } from 'lucide-react';

const initialItems = [
  {
    id: 1,
    title: 'Google Pixel 8 Pro (Obsidian)',
    category: 'tech',
    status: 'FOUND',
    location: 'Main University Library, 3rd Floor',
    time: '25 mins ago',
    image: '/assets/cat_tech.jpg',
    finderVerified: true,
    description: 'Found on study desk with black spigen case and metallic ring grip.'
  },
  {
    id: 2,
    title: 'Vintage Leather Bifold & Keys',
    category: 'wallets',
    status: 'REUNITED',
    location: 'Central Plaza Coffee Stand',
    time: '2 hours ago',
    image: '/assets/cat_wallet.jpg',
    finderVerified: true,
    description: 'Safely claimed via Android blind cryptographic verification code.'
  },
  {
    id: 3,
    title: 'Golden Retriever with Silver Tag',
    category: 'pets',
    status: 'LOST',
    location: 'Oakridge Park North Meadow',
    time: '45 mins ago',
    image: '/assets/cat_pet.jpg',
    finderVerified: false,
    description: 'Friendly 2-year old named Milo. Wearing blue reflective collar with QR medal.'
  },
  {
    id: 4,
    title: 'Sony WH-1000XM5 Headphones',
    category: 'tech',
    status: 'FOUND',
    location: 'Metro Line 2 / Terminal B',
    time: '3 hours ago',
    image: '/assets/cat_tech.jpg',
    finderVerified: true,
    description: 'Silver case found on transit seat. Handed to Station Lost Property Desk.'
  },
  {
    id: 5,
    title: 'Black Bellroy Card Sleeve & ID',
    category: 'wallets',
    status: 'LOST',
    location: 'Engineering Science Quad',
    time: '5 hours ago',
    image: '/assets/cat_wallet.jpg',
    finderVerified: false,
    description: 'Contains student smartcard and transport pass. Reward offered via app.'
  },
  {
    id: 6,
    title: 'Canvas Travel Bag & Notebooks',
    category: 'travel',
    status: 'REUNITED',
    location: 'Campus South Shuttle Bus',
    time: 'Yesterday',
    image: '/assets/about_reunion.jpg',
    finderVerified: true,
    description: 'Reunited at campus security locker within 90 minutes of reporting.'
  }
];

export default function RecentItemsFeed({ onOpenReport, onOpenDownload }) {
  const [filter, setFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedItem, setSelectedItem] = useState(null);

  const filteredItems = initialItems.filter(item => {
    const matchesFilter = 
      filter === 'ALL' ||
      (filter === 'LOST' && item.status === 'LOST') ||
      (filter === 'FOUND' && item.status === 'FOUND') ||
      (filter === 'REUNITED' && item.status === 'REUNITED');

    const matchesSearch = 
      item.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.location.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.description.toLowerCase().includes(searchQuery.toLowerCase());

    return matchesFilter && matchesSearch;
  });

  return (
    <section className="section-feed" id="feed">
      {/* Editorial Section Header */}
      <div className="section-header-editorial">
        <div>
          <span className="text-overline">REAL-TIME COMMUNITY DISCOVERY</span>
          <h2 className="text-section-title" style={{ marginTop: '8px' }}>RECENT REUNIONS</h2>
        </div>
        <button 
          onClick={onOpenReport}
          className="btn-terracotta"
          id="feed-post-report-btn"
          style={{ padding: '10px 18px', fontSize: '0.72rem' }}
        >
          Post Lost / Found
        </button>
      </div>

      {/* Filter and Search Bar */}
      <div className="feed-filters-bar">
        <div className="filter-pills">
          {['ALL', 'LOST', 'FOUND', 'REUNITED'].map(type => (
            <button
              key={type}
              onClick={() => setFilter(type)}
              className={`filter-btn ${filter === type ? 'active' : ''}`}
              id={`filter-btn-${type.toLowerCase()}`}
            >
              {type === 'ALL' ? 'All Activity' : `${type} Items`}
            </button>
          ))}
        </div>

        <div className="search-input-wrap">
          <Search size={16} className="search-icon-pos" />
          <input 
            type="text" 
            placeholder="Search items, locations, serials..." 
            className="search-input"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            id="items-search-input"
          />
        </div>
      </div>

      {/* Items Grid */}
      <div className="items-grid">
        {filteredItems.map(item => (
          <div key={item.id} className="item-feed-card" id={`item-card-${item.id}`}>
            <div className="item-card-media">
              <img src={item.image} alt={item.title} />
              <span className={`item-status-pill status-${item.status.toLowerCase()}`}>
                {item.status}
              </span>
            </div>

            <div className="item-card-body">
              <div>
                <h3 className="item-card-title">{item.title}</h3>
                <div className="item-card-meta">
                  <MapPin size={13} color="var(--terracotta)" />
                  <span>{item.location}</span>
                </div>
                <div className="item-card-meta">
                  <Clock size={13} />
                  <span>{item.time}</span>
                  {item.finderVerified && (
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '3px', color: '#279A67', marginLeft: '6px', fontWeight: 600 }}>
                      <ShieldCheck size={13} /> Verified
                    </span>
                  )}
                </div>
                <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)', lineHeight: '1.5', marginTop: '6px' }}>
                  {item.description}
                </p>
              </div>

              <div style={{ marginTop: '16px', paddingTop: '14px', borderTop: '1px solid var(--border-light)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <span style={{ fontSize: '0.72rem', color: 'var(--text-light)', fontWeight: 600 }}>
                  REF #{item.id}8294
                </span>
                <button 
                  onClick={() => {
                    setSelectedItem(item);
                  }}
                  className="editorial-link-btn"
                  style={{ background: 'none', border: 'none', cursor: 'pointer' }}
                >
                  <span>{item.status === 'FOUND' ? 'CLAIM ITEM' : 'VIEW DETAILS'}</span>
                  <ArrowUpRight size={13} />
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>

      {filteredItems.length === 0 && (
        <div style={{ textAlign: 'center', padding: '60px 20px', background: '#FFFFFF', borderRadius: '8px', border: '1px dashed var(--border-light)' }}>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.95rem' }}>No items match your search "{searchQuery}".</p>
          <button 
            onClick={() => { setSearchQuery(''); setFilter('ALL'); }}
            className="btn-outline" 
            style={{ marginTop: '14px' }}
          >
            Clear Filters
          </button>
        </div>
      )}

      {/* Claim / Detail Modal popup */}
      {selectedItem && (
        <div className="modal-overlay" onClick={() => setSelectedItem(null)}>
          <div className="modal-container" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div>
                <span className={`item-status-pill status-${selectedItem.status.toLowerCase()}`} style={{ position: 'static' }}>
                  {selectedItem.status}
                </span>
                <h3 style={{ fontFamily: 'var(--font-serif)', fontSize: '1.4rem', marginTop: '6px' }}>
                  {selectedItem.title}
                </h3>
              </div>
              <button className="modal-close-btn" onClick={() => setSelectedItem(null)}>✕</button>
            </div>
            <div className="modal-body">
              <img src={selectedItem.image} alt={selectedItem.title} style={{ width: '100%', height: '180px', objectFit: 'cover', borderRadius: '6px', marginBottom: '16px' }} />
              <p style={{ fontSize: '0.88rem', color: 'var(--text-muted)', marginBottom: '16px' }}>
                {selectedItem.description}
              </p>
              <div style={{ background: 'var(--bg-cream)', padding: '14px', borderRadius: '6px', marginBottom: '20px', fontSize: '0.82rem' }}>
                <div style={{ fontWeight: 600, color: 'var(--text-main)', marginBottom: '4px' }}>
                  Verification Protocol Required:
                </div>
                <div>To protect privacy, claimants must verify ownership details or scan the item badge using our Android app.</div>
              </div>
              <div style={{ display: 'flex', gap: '10px' }}>
                <button 
                  onClick={() => {
                    setSelectedItem(null);
                    onOpenDownload();
                  }}
                  className="btn-terracotta"
                  style={{ flex: 1 }}
                >
                  Verify in Android App
                </button>
                <button 
                  onClick={() => setSelectedItem(null)}
                  className="btn-outline"
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}
