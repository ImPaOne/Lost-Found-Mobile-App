import React, { useState } from 'react';
import { ArrowUpRight, Smartphone, Key, HeartHandshake, Briefcase, ChevronRight } from 'lucide-react';

const categoriesData = [
  {
    id: 'tech',
    category: 'TECH & PHONES',
    highlightTag: 'REAL-TIME MATCH',
    subtitle: 'Lost in transit, library, or cafes',
    image: '/assets/cat_tech.jpg',
    description: 'Automated IMEI and serial registration. Uses Android background BLE beacon scanners to alert you when your device comes within 50 meters.',
    stats: '1,420 phones found this month'
  },
  {
    id: 'wallets',
    category: 'WALLETS & KEYS',
    highlightTag: 'ANONYMOUS SAFE',
    subtitle: 'Cards, IDs, keychains & fobs',
    image: '/assets/cat_wallet.jpg',
    description: 'Double-blind verification protocol. Claimants describe internal card traits or key fobs without disclosing their full name or phone number.',
    stats: '98% identity protected'
  },
  {
    id: 'pets',
    category: 'PETS & ANIMALS',
    highlightTag: 'URGENT RADAR',
    subtitle: 'Companion dogs, cats & tags',
    image: '/assets/cat_pet.jpg',
    description: 'Instant 5km community broadcast. Every user running our Android app in the vicinity receives a high-priority photo alert to spot your pet.',
    stats: 'Average recovery < 2.5 hours'
  },
  {
    id: 'travel',
    category: 'BAGS & TRAVEL',
    highlightTag: 'TRANSIT SYNC',
    subtitle: 'Backpacks, luggage & coats',
    image: '/assets/about_reunion.jpg',
    description: 'Direct API integrations with university lost & found desks, municipal bus networks, and airport lost property repositories.',
    stats: '350+ official drop zones'
  }
];

export default function ServicesCategories({ onOpenReport, onSelectCategory }) {
  const [activeId, setActiveId] = useState('tech');

  return (
    <section className="section-services" id="categories">
      {/* Header matching Reference Image 4 */}
      <div className="section-header-editorial">
        <h2 className="text-section-title">CATEGORIES</h2>
        <button 
          onClick={onOpenReport}
          className="editorial-link-btn"
          id="categories-report-link"
          style={{ background: 'none', border: 'none', cursor: 'pointer' }}
        >
          <span>REPORT AN ITEM</span>
          <ArrowUpRight size={15} />
        </button>
      </div>

      {/* Editorial Row-by-Row Layout matching Reference Image 4 */}
      <div className="services-list">
        {categoriesData.map((item) => {
          const isActive = activeId === item.id;
          return (
            <div 
              key={item.id}
              className="service-row"
              onClick={() => {
                setActiveId(item.id);
                if (onSelectCategory) onSelectCategory(item);
              }}
              style={{
                backgroundColor: isActive ? 'var(--bg-cream)' : 'transparent',
                borderLeft: isActive ? '3px solid var(--terracotta)' : '3px solid transparent',
                paddingLeft: isActive ? '16px' : '0'
              }}
              id={`category-row-${item.id}`}
            >
              {/* Left Column: Category Name & Tag */}
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                  {isActive ? (
                    <span className="tag-highlight-pill">{item.highlightTag}</span>
                  ) : (
                    <span className="text-overline" style={{ fontSize: '0.7rem' }}>{item.category}</span>
                  )}
                </div>
                <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                  {item.subtitle}
                </div>
              </div>

              {/* Middle Column: Editorial Thumbnail Photo */}
              <div className="service-image-box">
                <img src={item.image} alt={item.category} />
              </div>

              {/* Right Column: Editorial Description */}
              <div>
                <p className="service-description">
                  {item.description}
                </p>
                <div style={{ fontSize: '0.7rem', color: 'var(--terracotta)', fontWeight: 600, marginTop: '4px' }}>
                  {item.stats}
                </div>
              </div>

              {/* Far Right: Arrow Action Circle */}
              <div>
                <button 
                  className="service-arrow-btn"
                  title="View category details"
                  onClick={(e) => {
                    e.stopPropagation();
                    setActiveId(item.id);
                    onOpenReport();
                  }}
                >
                  <ArrowUpRight size={18} />
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}
