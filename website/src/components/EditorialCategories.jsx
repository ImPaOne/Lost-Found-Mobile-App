import React, { useState } from 'react';
import { ArrowUpRight } from 'lucide-react';
import { playClickSound } from '../utils/audio';

const categories = [
  {
    num: '01',
    category: 'TECH & SMARTPHONES',
    highlightTag: 'BLUETOOTH 5.3 RADAR',
    subtitle: 'Phones, laptops, wireless earbuds, smartwatches',
    image: '/assets/cat_tech.jpg',
    description: 'Automatic hardware beacon triangulation. When any nearby Android user running FOUNDIT passes within 50m of your device, you receive a silent, encrypted location update.',
    metric: '1,420 devices reunited this month'
  },
  {
    num: '02',
    category: 'WALLETS & IDENTITIES',
    highlightTag: 'ZERO-KNOWLEDGE VERIFY',
    subtitle: 'Bifolds, passports, cards, badges & IDs',
    image: '/assets/cat_wallet.jpg',
    description: 'Claimants provide double-blind proof of ownership (color, internal cards, or initial stamps) through single-use tokens without disclosing their phone number or home address.',
    metric: '100% identity privacy guaranteed'
  },
  {
    num: '03',
    category: 'PETS & COMPANIONS',
    highlightTag: 'URGENT GEOFENCE 5KM',
    subtitle: 'Dogs, cats & tagged companion animals',
    image: '/assets/cat_pet.jpg',
    description: 'Broadcasts high-priority visual alerts to every Android device within a 5-kilometer radius. Scan any pet collar QR tag with an Android camera for immediate owner notification.',
    metric: 'Average reunion in < 2.5 hours'
  },
  {
    num: '04',
    category: 'KEYS & COMMUTE GEAR',
    highlightTag: 'CAMPUS SYNC',
    subtitle: 'Keychains, fobs, backpacks & luggage',
    image: '/assets/about_reunion.jpg',
    description: 'Integrated directly with university security desks, campus shuttles, and municipal transit repositories for seamless contactless returns.',
    metric: '350+ verified return hubs'
  }
];

export default function EditorialCategories({ onDownloadApk }) {
  const [activeIdx, setActiveIdx] = useState(0);

  return (
    <section className="editorial-services-section" id="categories">
      <div className="container-fullscreen">
        {/* Header directly matching Reference Image 4 */}
        <div className="services-editorial-header">
          <h2 className="services-title-large">CATEGORIES</h2>
          <button 
            onClick={onDownloadApk}
            className="services-header-link"
          >
            <span>DOWNLOAD ANDROID RADAR</span>
            <ArrowUpRight size={16} />
          </button>
        </div>

        {/* Row by row layout directly matching Reference Image 4 */}
        <div className="services-editorial-list">
          {categories.map((cat, idx) => {
            const isActive = activeIdx === idx;
            return (
              <div 
                key={cat.num}
                className={`service-editorial-row ${isActive ? 'row-active' : ''}`}
                onMouseEnter={() => {
                  if (activeIdx !== idx) {
                    playClickSound();
                    setActiveIdx(idx);
                  }
                }}
                onClick={() => {
                  playClickSound();
                  setActiveIdx(idx);
                }}
              >
                {/* Col 1: Category Name & Tag */}
                <div className="service-col-name">
                  <div className="service-tag-wrapper">
                    <span className="service-number">{cat.num}</span>
                    {isActive ? (
                      <span className="service-active-pill">{cat.highlightTag}</span>
                    ) : (
                      <span className="service-category-name">{cat.category}</span>
                    )}
                  </div>
                  <div className="service-sub-label">{cat.subtitle}</div>
                </div>

                {/* Col 2: Image Thumbnail */}
                <div className="service-col-image">
                  <img src={cat.image} alt={cat.category} className="service-thumb-img" />
                </div>

                {/* Col 3: Description & Metric */}
                <div className="service-col-desc">
                  <p className="service-desc-text">{cat.description}</p>
                  <div className="service-metric-text">{cat.metric}</div>
                </div>

                {/* Col 4: Arrow Action */}
                <div className="service-col-arrow">
                  <div className="service-round-arrow">
                    <ArrowUpRight size={18} />
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
