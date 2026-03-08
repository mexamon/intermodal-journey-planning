import React, { useState, useMemo } from 'react';
import * as s from './KnowledgeBase.module.scss';
import { FiSearch, FiChevronRight, FiArrowLeft, FiBookOpen, FiLayers, FiDollarSign, FiDatabase, FiShield } from 'react-icons/fi';
import { MdFlight } from 'react-icons/md';

/* ──────────────────────────────────────────────
   DIAGRAM HELPERS — styled flow boxes
   ────────────────────────────────────────────── */
const dc = { // diagram colors
  blue: '#3b82f6', green: '#22c55e', purple: '#8b5cf6', amber: '#f59e0b',
  red: '#C8102E', gray: '#6b7280', teal: '#14b8a6', rose: '#f43f5e',
};

const Box: React.FC<{ label: string; sub?: string; color?: string; small?: boolean; w?: string }> = ({ label, sub, color = dc.blue, small, w }) => (
  <div style={{
    padding: small ? '0.35rem 0.6rem' : '0.5rem 0.8rem',
    borderRadius: 8, border: `1.5px solid ${color}30`,
    background: `${color}08`, textAlign: 'center',
    fontSize: small ? '0.68rem' : '0.75rem', fontWeight: 600, fontFamily: '"Manrope",sans-serif',
    color, lineHeight: 1.3, width: w, flexShrink: 0,
  }}>
    {label}
    {sub && <div style={{ fontSize: '0.62rem', fontWeight: 400, opacity: 0.7, marginTop: 2 }}>{sub}</div>}
  </div>
);

const Arrow: React.FC<{ label?: string; horizontal?: boolean }> = ({ label, horizontal }) => (
  <div style={{
    display: 'flex', flexDirection: horizontal ? 'row' : 'column',
    alignItems: 'center', gap: 0, opacity: 0.35,
    ...(horizontal ? { margin: '0 0.15rem' } : { margin: '0.15rem 0' }),
  }}>
    {!horizontal && <div style={{ width: 1.5, height: label ? 10 : 14, background: 'currentColor' }} />}
    {horizontal && <div style={{ height: 1.5, width: label ? 10 : 14, background: 'currentColor' }} />}
    {label && <span style={{ fontSize: '0.58rem', fontWeight: 600, margin: '0 0.2rem', whiteSpace: 'nowrap' }}>{label}</span>}
    {!horizontal ? <span style={{ fontSize: 9, lineHeight: 1 }}>▼</span> : <span style={{ fontSize: 9, lineHeight: 1 }}>▶</span>}
  </div>
);

const FlowCol: React.FC<{ children: React.ReactNode; align?: string }> = ({ children, align = 'center' }) => (
  <div style={{ display: 'flex', flexDirection: 'column', alignItems: align as any, gap: 0 }}>{children}</div>
);

const FlowRow: React.FC<{ children: React.ReactNode; gap?: string; wrap?: boolean }> = ({ children, gap = '0.5rem', wrap }) => (
  <div style={{ display: 'flex', alignItems: 'center', gap, justifyContent: 'center', flexWrap: wrap ? 'wrap' : undefined }}>{children}</div>
);

const DiagramWrap: React.FC<{ children: React.ReactNode; title?: string }> = ({ children, title }) => (
  <div style={{
    padding: '1rem 1.2rem', borderRadius: 10,
    border: '1px solid rgba(128,128,128,0.1)',
    background: 'rgba(128,128,128,0.02)',
    margin: '0.8rem 0',
  }}>
    {title && <div style={{ fontSize: '0.62rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em', opacity: 0.35, marginBottom: '0.6rem', fontFamily: '"Manrope",sans-serif' }}>{title}</div>}
    {children}
  </div>
);

const LayerBox: React.FC<{ label: string; color: string; items: string[] }> = ({ label, color, items }) => (
  <div style={{
    padding: '0.5rem 0.7rem', borderRadius: 8, border: `1.5px solid ${color}25`,
    background: `${color}06`, marginBottom: '0.35rem',
  }}>
    <div style={{ fontSize: '0.62rem', fontWeight: 700, color, marginBottom: '0.3rem', fontFamily: '"Manrope",sans-serif' }}>{label}</div>
    <div style={{ display: 'flex', gap: '0.3rem', flexWrap: 'wrap' }}>
      {items.map(item => (
        <span key={item} style={{
          fontSize: '0.65rem', fontWeight: 500, padding: '0.15rem 0.4rem',
          borderRadius: 4, background: `${color}10`, color, fontFamily: 'ui-monospace, monospace',
        }}>{item}</span>
      ))}
    </div>
  </div>
);

/* ──────────────────────────────────────────────
   DIAGRAM COMPONENTS (replacing ASCII)
   ────────────────────────────────────────────── */
const ArchDiagram: React.FC = () => (
  <DiagramWrap title="System Architecture">
    <FlowCol>
      <Box label="Frontend" sub="React + TypeScript" color={dc.blue} />
      <FlowRow gap="0.35rem"><Box label="Planner" color={dc.blue} small /><Box label="Manage" color={dc.blue} small /><Box label="Admin" color={dc.blue} small /><Box label="KB" color={dc.blue} small /></FlowRow>
      <Arrow label="REST API" />
      <Box label="Backend" sub="Spring Boot" color={dc.green} />
      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.2rem', margin: '0.2rem 0', width: '100%', maxWidth: 340, alignSelf: 'center' }}>
        {[
          { l: 'Controller Layer', s: 'REST endpoints', c: dc.green },
          { l: 'Service Layer', s: 'Business logic', c: dc.green },
          { l: 'Resolver Chain', s: 'Static · Amadeus · Computed · GTFS', c: dc.purple },
          { l: 'Repository Layer', s: 'JPA / Spring Data', c: dc.green },
        ].map((x, i) => (
          <React.Fragment key={i}>
            <Arrow />
            <Box label={x.l} sub={x.s} color={x.c} />
          </React.Fragment>
        ))}
      </div>
      <Arrow label="JDBC" />
      <Box label="PostgreSQL + PostGIS" color={dc.amber} />
      <div style={{ display: 'flex', gap: '0.3rem', marginTop: '0.3rem', justifyContent: 'center', flexWrap: 'wrap' }}>
        <LayerBox label="L1 Reference" color={dc.gray} items={['ref_country', 'ref_region']} />
        <LayerBox label="L2 Inventory" color={dc.teal} items={['location', 'provider']} />
        <LayerBox label="L3 Transport" color={dc.purple} items={['edge', 'edge_trip', 'service_area']} />
        <LayerBox label="L4 Fare" color={dc.red} items={['fare']} />
      </div>
    </FlowCol>
  </DiagramWrap>
);

const SearchPipelineDiagram: React.FC = () => (
  <DiagramWrap title="Search Pipeline">
    <FlowCol>
      {[
        { l: '1. Location Resolution', s: 'IATA → DB, UUID → DB, Coords → Virtual', c: dc.blue },
        { l: '2. Hub Discovery', s: '50km radius → nearby airports/stations', c: dc.teal },
        { l: '3. Edge Resolution (3-Phase)', s: '', c: dc.purple },
      ].map((x, i) => (
        <React.Fragment key={i}>
          {i > 0 && <Arrow />}
          <Box label={x.l} sub={x.s} color={x.c} />
        </React.Fragment>
      ))}
      <div style={{ display: 'flex', gap: '0.35rem', margin: '0.3rem 0', justifyContent: 'center', flexWrap: 'wrap' }}>
        <Box label="Phase A" sub="Origin → Hubs (COMPUTED)" color={dc.green} small />
        <Box label="Phase B" sub="Hub ↔ Hub BFS (STATIC + AMADEUS)" color={dc.blue} small />
        <Box label="Phase C" sub="Hubs → Dest (COMPUTED)" color={dc.green} small />
      </div>
      <Arrow />
      <Box label="4. BFS Path Finding" sub="Time-aware, transfer limits, cycle detection" color={dc.amber} />
      <Arrow />
      <Box label="5. Rank & Label" sub="En Hızlı · En Ucuz · En Yeşil · En Az Aktarma" color={dc.red} />
      <Arrow />
      <Box label="Journey Results" sub="segments, cost, duration, CO₂" color={dc.gray} />
    </FlowCol>
  </DiagramWrap>
);

const PriceFlowDiagram: React.FC = () => (
  <DiagramWrap title="Price Resolution Flow">
    <FlowCol>
      <Box label="Journey Search" color={dc.gray} />
      <Arrow />
      <FlowRow gap="0.5rem" wrap>
        <FlowCol>
          <Box label="StaticEdgeResolver" sub="✈️ 🚆 🚌 ⛴️" color={dc.blue} />
          <Arrow />
          <Box label="fare table (DB)" sub="class-based pricing" color={dc.blue} small />
        </FlowCol>
        <FlowCol>
          <Box label="AmadeusEdgeResolver" sub="✈️ 3rd party" color={dc.amber} />
          <Arrow />
          <Box label="Amadeus API" sub="real-time offers" color={dc.amber} small />
        </FlowCol>
        <FlowCol>
          <Box label="ComputedEdgeResolver" sub="🚕 🚶" color={dc.green} />
          <Arrow />
          <Box label="service_area" sub="config_json formula" color={dc.green} small />
        </FlowCol>
      </FlowRow>
    </FlowCol>
  </DiagramWrap>
);

const SchemaLayerDiagram: React.FC = () => (
  <DiagramWrap title="Database Schema Layers">
    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.3rem' }}>
      <LayerBox label="Layer 4 — Fare" color={dc.red} items={['fare → edge + trip + class + price']} />
      <LayerBox label="Layer 3 — Transport" color={dc.purple} items={['transport_mode', 'transportation_edge', 'edge_trip', 'transport_service_area']} />
      <LayerBox label="Layer 2 — Inventory" color={dc.teal} items={['location', 'provider']} />
      <LayerBox label="Layer 1 — Reference" color={dc.gray} items={['ref_country', 'ref_region']} />
    </div>
    <div style={{ fontSize: '0.65rem', opacity: 0.4, marginTop: '0.4rem', textAlign: 'center' }}>↑ FK dependencies flow upward — lower layers are referenced by upper layers</div>
  </DiagramWrap>
);

const ResolverDiagram: React.FC = () => (
  <DiagramWrap title="Edge Resolution by Mode">
    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.3rem' }}>
      {[
        { mode: 'STATIC', desc: 'Pre-defined DB edges', resolver: 'StaticEdgeResolver', modes: '✈️ FLIGHT  🚆 TRAIN  🚌 BUS  ⛴️ FERRY', c: dc.blue },
        { mode: 'COMPUTED', desc: 'Haversine + service_area', resolver: 'ComputedEdgeResolver', modes: '🚕 UBER  🚶 WALKING', c: dc.green },
        { mode: 'API_DYNAMIC', desc: 'External API query', resolver: 'AmadeusEdgeResolver', modes: '✈️ Amadeus flight search', c: dc.amber },
      ].map(x => (
        <div key={x.mode} style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.4rem 0.6rem', borderRadius: 8, border: `1px solid ${x.c}20`, background: `${x.c}04` }}>
          <span style={{ fontSize: '0.68rem', fontWeight: 700, color: x.c, minWidth: 75, fontFamily: '"Manrope",sans-serif' }}>{x.mode}</span>
          <span style={{ fontSize: '0.62rem', fontWeight: 600, opacity: 0.5, flex: 1 }}>{x.desc}</span>
          <span style={{ fontSize: '0.6rem', opacity: 0.4 }}>{x.modes}</span>
        </div>
      ))}
    </div>
  </DiagramWrap>
);

const GeofenceDiagram: React.FC = () => (
  <DiagramWrap title="Service Area Priority Matching">
    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
      {[
        { p: '4', type: 'RADIUS', desc: 'Coordinate + radius match', ex: 'IST Airport 30km zone', c: dc.red },
        { p: '3', type: 'CITY', desc: 'City name string match', ex: 'Istanbul Metro Zone', c: dc.amber },
        { p: '2', type: 'COUNTRY', desc: 'ISO country code', ex: 'Turkey National', c: dc.blue },
        { p: '1', type: 'GLOBAL', desc: 'Always matches', ex: 'Worldwide fallback', c: dc.gray },
      ].map(x => (
        <div key={x.type} style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.4rem 0.6rem', borderRadius: 8, background: `${x.c}06`, border: `1px solid ${x.c}18` }}>
          <span style={{ fontSize: '0.72rem', fontWeight: 800, color: x.c, width: 18, textAlign: 'center' }}>{x.p}</span>
          <span style={{ fontSize: '0.7rem', fontWeight: 700, color: x.c, minWidth: 65, fontFamily: '"Manrope",sans-serif' }}>{x.type}</span>
          <span style={{ fontSize: '0.65rem', fontWeight: 500, opacity: 0.5, flex: 1 }}>{x.desc}</span>
          <span style={{ fontSize: '0.6rem', opacity: 0.35, fontStyle: 'italic' }}>{x.ex}</span>
        </div>
      ))}
    </div>
    <div style={{ fontSize: '0.62rem', opacity: 0.35, marginTop: '0.35rem', textAlign: 'center' }}>Highest priority match wins — most specific area takes precedence</div>
  </DiagramWrap>
);

/* ──────────────────────────────────────────────
   ARTICLES DATA
   ────────────────────────────────────────────── */
interface ArticleData { id: string; title: string; category: string; tags: string[]; markdown: string; diagrams?: React.ReactNode; }

const ARTICLES: ArticleData[] = [
  {
    id: 'arch-overview', category: 'architecture', title: 'System Architecture Overview',
    tags: ['architecture', 'overview', 'layers'],
    diagrams: <ArchDiagram />,
    markdown: `
# System Architecture Overview

Intermodal Journey Planning sistemi, çok modlu ulaşım rotalarını optimize eden bir arama motorudur.

## Resolver Chain Architecture

Arama motoru **resolver pattern** kullanır. Her resolver bağımsız çalışır ve \`ResolvedEdge\` üretir:

- **StaticEdgeResolver** — DB'deki \`transportation_edge\` + \`edge_trip\` kayıtları (uçuş, tren, otobüs)
- **AmadeusEdgeResolver** — Amadeus API'dan gerçek zamanlı uçuş arama
- **ComputedEdgeResolver** — Haversine hesaplamasıyla taksi/yürüyüş rotaları
- **GtfsEdgeResolver** — GTFS verisiyle toplu taşıma (gelecek)

## 3-Phase Search

1. **Phase A (First-Mile):** Kullanıcı konumundan en yakın hub'lara — COMPUTED resolver
2. **Phase B (Trunk):** Hub'lar arası BFS — STATIC + AMADEUS resolver
3. **Phase C (Last-Mile):** Varış hub'larından hedefe — COMPUTED resolver
    `
  },
  {
    id: 'arch-search-pipeline', category: 'architecture', title: 'Journey Search Pipeline',
    tags: ['search', 'BFS', 'pipeline', 'resolver'],
    diagrams: <SearchPipelineDiagram />,
    markdown: `
# Journey Search Pipeline

## Transfer Times

| Mode → Transfer at | Wait Time |
|---|---|
| ✈️ Flight arrival | 60 min |
| 🚆 Train arrival | 15 min |
| 🚌 Bus arrival | 10 min |
| 🚇 Subway arrival | 5 min |
| ⛴️ Ferry arrival | 15 min |
| 🚕 Taxi arrival | 5 min |
| 🚶 Walking | 0 min |
    `
  },
  {
    id: 'pricing-overview', category: 'pricing', title: 'Pricing Policy Overview',
    tags: ['pricing', 'fare', 'cost', 'fiyat'],
    diagrams: <PriceFlowDiagram />,
    markdown: `
# Pricing Policy Overview

Sistemde fiyatlandırma **4 farklı kaynaktan** gelir. Her ulaşım modu kendi fiyat kaynağını kullanır.

## Fiyat Kaynakları

| Kaynak | Nerede | Kapsam | Detay |
|---|---|---|---|
| **fare table** | \`fare\` tablosu | THY uçuşları, tren, otobüs, feribot | Class, refund, luggage |
| **edge_trip.estimated_cost_cents** | \`edge_trip\` tablosu | Tüm static edge'ler | Tek rakam (kaba tahmin) |
| **service_area.config_json.pricing** | \`transport_service_area\` | Taksi, UBER, scooter | Formula: base + per_km |
| **Amadeus API** | Harici API | 3. parti havayolları | Gerçek zamanlı fiyat |

## Temel Kural

> **Fiyatı biz kontrol ediyorsak** → \`fare\` tablosu
> **Fiyat dışarıdan geliyorsa** → API veya formula

## Mode Bazlı Fiyatlandırma

| Mod | Fiyat Kaynağı | Yönetim |
|---|---|---|
| ✈️ THY Uçuşları | fare tablosu | Fares ekranı |
| ✈️ Diğer Havayolları | Amadeus API | Otomatik |
| 🚆 Tren | fare tablosu | Fares ekranı |
| 🚌 Otobüs | fare tablosu | Fares ekranı |
| ⛴️ Feribot | fare tablosu | Fares ekranı |
| 🚕 Taksi/UBER | service_area formülü | Service Areas ekranı |
| 🚶 Yürüyüş | Ücretsiz | — |
    `
  },
  {
    id: 'pricing-fare-table', category: 'pricing', title: 'Fare Table & Classes',
    tags: ['fare', 'class', 'economy', 'business', 'first'],
    markdown: `
# Fare Table & Classes

\`fare\` tablosu, statik fiyatları yönetir. Her kayıt bir \`transportation_edge\` + opsiyonel \`edge_trip\` + \`fare_class\` kombinasyonuna bağlıdır.

## Fare Classes

| Class | Açıklama | Tipik Kullanım |
|---|---|---|
| **ECONOMY** | Ekonomi | Uçuş, uzun mesafe tren |
| **PREMIUM_ECONOMY** | Premium Ekonomi | Uçuş |
| **BUSINESS** | Business | Uçuş, premium tren |
| **FIRST** | First Class | Uçuş |
| **STANDARD** | Standart | Otobüs, metro, feribot |
| **COMFORT** | Konforlu | Otobüs, feribot |
| **VIP** | VIP | Özel servis |

## Pricing Types

| Tip | Açıklama | Fiyat Alanı |
|---|---|---|
| **FIXED** | Sabit fiyat | \`price_cents\` alanından |
| **ESTIMATED** | Tahmini | \`price_cents\` (referans) |
| **DYNAMIC** | Dinamik | API'dan gelir |
| **FREE** | Ücretsiz | 0 |

## Uniqueness Constraint

Aynı edge + trip + fareClass kombinasyonu tekrar girilemez. \`@UniqueFare\` validator'ı DB'den kontrol eder.
    `
  },
  {
    id: 'pricing-service-area', category: 'pricing', title: 'Service Area Pricing Formulas',
    tags: ['service area', 'taxi', 'uber', 'formula', 'pricing'],
    markdown: `
# Service Area Pricing Formulas

Taksi ve UBER gibi on-demand modlar için fiyatlar \`service_area.config_json.pricing\` formülüyle hesaplanır.

## Formula

\`\`\`
cost = base_fare_cents + (distance_km × per_km_cents)
cost = max(cost, min_fare_cents)
\`\`\`

## config_json Örneği

\`\`\`json
{
  "max_distance_m": 80000,
  "pricing": {
    "base_fare_cents": 3000,
    "per_km_cents": 350,
    "min_fare_cents": 5000
  }
}
\`\`\`

Bu örnekte:
- **Biniş ücreti:** ₺30
- **Km başı:** ₺3.50
- **Minimum ücret:** ₺50
- 20km yolculuk: ₺30 + (20 × ₺3.50) = **₺100**
    `
  },
  {
    id: 'data-schema', category: 'data', title: 'Database Schema Layers',
    tags: ['database', 'schema', 'tables', 'layers'],
    diagrams: <SchemaLayerDiagram />,
    markdown: `
# Database Schema Layers

Veritabanı 4 katmanlı bir şema kullanır. Alt katmanlar üst katmanlara FK ile bağlıdır.

## Katman Detayları

- **L1 Reference:** Sabit referans verileri — ülkeler, bölgeler (ISO standartları)
- **L2 Inventory:** İş nesneleri — lokasyonlar (havalimanı, istasyon), sağlayıcılar (havayolu, demiryolu)
- **L3 Transport:** Ulaşım ağı — modlar, A→B bağlantıları (edge), seferler (trip), servis alanları
- **L4 Fare:** Fiyatlandırma — sınıf bazlı fiyatlar, iade politikaları
    `
  },
  {
    id: 'data-edges-trips', category: 'data', title: 'Edges & Trips Explained',
    tags: ['edge', 'trip', 'connection', 'schedule'],
    markdown: `
# Edges & Trips

## Transportation Edge

Bir \`transportation_edge\`, iki lokasyon arasındaki ulaşım bağlantısını temsil eder.

| Alan | Açıklama |
|---|---|
| origin_location_id | Kalkış noktası |
| destination_location_id | Varış noktası |
| transport_mode_id | Ulaşım modu |
| provider_id | Sağlayıcı (opsiyonel) |
| schedule_type | FIXED (sefer bazlı) veya FREQUENCY (sıklık bazlı) |
| status | ACTIVE / INACTIVE |

## Edge Trip

Bir \`edge_trip\`, belirli bir seferi temsil eder (FIXED schedule edge'ler için):

| Alan | Açıklama |
|---|---|
| edge_id | Bağlı olduğu edge |
| service_code | TK1592, PC2230 gibi sefer kodu |
| departure_time | Kalkış saati |
| arrival_time | Varış saati |
| operating_days_mask | Bitmask (Mon=1, Tue=2, Wed=4...) |
| estimated_cost_cents | Tahmini fiyat |

## Schedule Types

- **FIXED:** Belirli sefer saatleri (uçuş, şehirlerarası tren) → edge_trip kayıtları
- **FREQUENCY:** Dakika bazlı sıklık (metro her 5dk gibi) → edge.frequency_minutes
    `
  },
  {
    id: 'transport-modes', category: 'transport', title: 'Transport Mode Registry',
    tags: ['mode', 'flight', 'train', 'bus', 'taxi', 'walking'],
    diagrams: <ResolverDiagram />,
    markdown: `
# Transport Mode Registry

Her ulaşım modu \`transport_mode\` tablosunda tanımlanır.

## Coverage Types

| Tip | Açıklama | Edge Türü |
|---|---|---|
| **FIXED_STOP** | Sabit duraklar arası | Static edge'ler |
| **ON_DEMAND** | Talep bazlı | Computed edge'ler |

## Mode Configurations

Her mod için yapılandırılabilen alanlar:
- \`max_walking_access_m\` — Bu moda erişim için max yürüme mesafesi
- \`default_speed_kmh\` — Hız tahmini (computed edge'ler için)
- \`api_provider\` — Harici API entegrasyonu
- \`config_json\` — Mod bazlı özel ayarlar
    `
  },
  {
    id: 'policies-service-areas', category: 'policies', title: 'Service Areas & Geofencing',
    tags: ['service area', 'geofencing', 'zone', 'area'],
    diagrams: <GeofenceDiagram />,
    markdown: `
# Service Areas & Geofencing

Service area'lar, ulaşım modlarının hangi coğrafi bölgelerde aktif olduğunu tanımlar.

## config_json

Her service area'nın \`config_json\` JSONB alanında:
- **Pricing formülü** — base_fare, per_km, min_fare
- **Mesafe limiti** — max_distance_m
- **Özel ayarlar** — surge multiplier, operating hours vb.
    `
  },
  {
    id: 'policies-validation', category: 'policies', title: 'Validation & Data Integrity',
    tags: ['validation', 'constraint', 'unique', 'integrity'],
    markdown: `
# Validation & Data Integrity

Sistem, veri tutarlılığını Jakarta Bean Validation + custom constraint'lerle sağlar.

## Custom Validators

| Validator | Amaç | Seviye |
|---|---|---|
| \`@UniqueFare\` | Aynı edge+trip+class tekrar girilemesin | Class-level |
| \`@JourneySearchValidation\` | Arama parametreleri geçerli olsun | Class-level |

## Standard Annotations

- \`@NotNull\` — Zorunlu alanlar
- \`@NotBlank\` — Boş string engelleme
- \`@Size(min, max)\` — Uzunluk sınırı (ör. currency 3 karakter)

## Delete Protection

Provider ve transport mode gibi referans veriler silinirken, bağlı edge'ler varsa silme engellenir.

## Soft Delete

Tüm entity'ler \`deleted\` flag'i ile soft delete yapılır. \`findAllActive()\` sorguları \`WHERE deleted = false\` kullanır.
    `
  },
];

/* ──────────────────────────────────────────────
   COMPONENT
   ────────────────────────────────────────────── */
interface Category { id: string; label: string; icon: React.ReactNode; color: string; description: string; }

const CATEGORIES: Category[] = [
  { id: 'architecture', label: 'System Architecture', icon: <FiLayers size={18} />, color: '#3b82f6', description: 'Core architecture, search pipeline, and resolver chains' },
  { id: 'pricing', label: 'Pricing Policy', icon: <FiDollarSign size={18} />, color: '#22c55e', description: 'How fares are resolved across different transport modes' },
  { id: 'data', label: 'Data Management', icon: <FiDatabase size={18} />, color: '#8b5cf6', description: 'Locations, connections, edges, trips, and fare records' },
  { id: 'transport', label: 'Transport Modes', icon: <MdFlight size={18} />, color: '#f59e0b', description: 'Flight, train, bus, ferry, taxi, and walking configurations' },
  { id: 'policies', label: 'Business Policies', icon: <FiShield size={18} />, color: '#ef4444', description: 'Service areas, geofencing, and operational rules' },
];

export const KnowledgeBasePane: React.FC = () => {
  const [search, setSearch] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [selectedArticle, setSelectedArticle] = useState<ArticleData | null>(null);

  const filtered = useMemo(() => {
    let result = ARTICLES;
    if (selectedCategory) result = result.filter(a => a.category === selectedCategory);
    if (search) {
      const q = search.toLowerCase();
      result = result.filter(a =>
        a.title.toLowerCase().includes(q) ||
        a.tags.some(t => t.includes(q)) ||
        a.markdown.toLowerCase().includes(q)
      );
    }
    return result;
  }, [search, selectedCategory]);

  const categoryCounts = useMemo(() => {
    const c: Record<string, number> = {};
    ARTICLES.forEach(a => { c[a.category] = (c[a.category] || 0) + 1; });
    return c;
  }, []);

  /* ── Article detail ── */
  if (selectedArticle) {
    const cat = CATEGORIES.find(c => c.id === selectedArticle.category);
    return (
      <div className={s.detail}>
        <button onClick={() => setSelectedArticle(null)} className={s.backBtn}>
          <FiArrowLeft size={15} /> Back to Knowledge Base
        </button>
        {cat && (
          <span className={s.detailBadge} style={{ backgroundColor: `${cat.color}15`, color: cat.color }}>
            {cat.icon} {cat.label}
          </span>
        )}
        {/* Diagrams render as React components ABOVE markdown text */}
        {selectedArticle.diagrams}
        <div className={s.articleContent} dangerouslySetInnerHTML={{ __html: renderMarkdown(selectedArticle.markdown) }} />
      </div>
    );
  }

  /* ── Main view ── */
  return (
    <div className={s.root}>
      <div className={s.hero}>
        <div className={s.heroIcon}><FiBookOpen size={24} color="white" /></div>
        <h1 className={s.heroTitle}>Knowledge Base</h1>
        <p className={s.heroSub}>System architecture, pricing policies, and operational documentation</p>
      </div>

      <div className={s.searchWrap}>
        <FiSearch size={16} className={s.searchIcon} />
        <input className={s.searchInput} placeholder="Search articles..."
          value={search} onChange={e => { setSearch(e.target.value); setSelectedCategory(null); }} />
      </div>

      <div className={s.pillBar}>
        <button className={`${s.pill} ${!selectedCategory ? s.active : ''}`}
          onClick={() => setSelectedCategory(null)}
          style={!selectedCategory ? { borderColor: 'rgba(200,16,46,0.4)', backgroundColor: '#C8102E15', color: '#C8102E', opacity: 1 } : {}}>
          All ({ARTICLES.length})
        </button>
        {CATEGORIES.map(cat => (
          <button key={cat.id}
            className={`${s.pill} ${selectedCategory === cat.id ? s.active : ''}`}
            onClick={() => setSelectedCategory(selectedCategory === cat.id ? null : cat.id)}
            style={selectedCategory === cat.id ? { borderColor: `${cat.color}60`, backgroundColor: `${cat.color}15`, color: cat.color, opacity: 1 } : {}}>
            {cat.icon} {cat.label} ({categoryCounts[cat.id] || 0})
          </button>
        ))}
      </div>

      {!search && !selectedCategory && (
        <div className={s.cardGrid}>
          {CATEGORIES.map(cat => (
            <button key={cat.id} className={s.categoryCard} onClick={() => setSelectedCategory(cat.id)}>
              <div className={s.cardHead}>
                <span className={s.cardIcon} style={{ backgroundColor: `${cat.color}12`, color: cat.color }}>{cat.icon}</span>
                <div>
                  <div className={s.cardTitle}>{cat.label}</div>
                  <div className={s.cardCount}>{categoryCounts[cat.id] || 0} articles</div>
                </div>
              </div>
              <p className={s.cardDesc}>{cat.description}</p>
            </button>
          ))}
        </div>
      )}

      {(search || selectedCategory) && (
        <div className={s.articleList}>
          {filtered.length === 0 && <div className={s.noResults}>No articles found.</div>}
          {filtered.map(article => {
            const cat = CATEGORIES.find(c => c.id === article.category);
            return (
              <button key={article.id} className={s.articleRow} onClick={() => setSelectedArticle(article)}>
                <div>
                  <div className={s.articleMeta}>
                    {cat && <span className={s.articleBadge} style={{ backgroundColor: `${cat.color}12`, color: cat.color }}>{cat.label}</span>}
                    <span className={s.articleTitle}>{article.title}</span>
                  </div>
                  <div className={s.tagRow}>
                    {article.tags.slice(0, 4).map(t => <span key={t} className={s.tag}>{t}</span>)}
                  </div>
                </div>
                <FiChevronRight size={16} className={s.chevron} />
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
};

/* ── Markdown → HTML ── */
function renderMarkdown(md: string): string {
  let html = md.trim();
  html = html.replace(/```(\w*)\n([\s\S]*?)```/g, (_m, _lang, code) =>
    `<pre><code>${escHtml(code.trim())}</code></pre>`);
  html = html.replace(/`([^`]+)`/g, '<code>$1</code>');
  html = html.replace(/^### (.+)$/gm, '<h3>$1</h3>');
  html = html.replace(/^## (.+)$/gm, '<h2>$1</h2>');
  html = html.replace(/^# (.+)$/gm, '<h1>$1</h1>');
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>');
  html = html.replace(/^> (.+)$/gm, '<blockquote><p>$1</p></blockquote>');
  html = html.replace(/^\|(.+)\|$/gm, (_, row: string) => {
    const cells = row.split('|').map((c: string) => c.trim());
    if (cells.every((c: string) => /^-+$/.test(c))) return '<!-- sep -->';
    return cells.map((c: string) => `<td>${c}</td>`).join('');
  });
  html = html.replace(/((?:<td>.*<\/td>\n?)+)/g, (block) => {
    const rows = block.trim().split('\n').filter((r: string) => r && !r.includes('<!-- sep -->'));
    if (rows.length === 0) return '';
    const hdr = rows[0]; const body = rows.slice(1);
    return `<table><thead><tr>${hdr.replace(/<td>/g, '<th>').replace(/<\/td>/g, '</th>')}</tr></thead><tbody>${body.map((r: string) => `<tr>${r}</tr>`).join('')}</tbody></table>`;
  });
  html = html.replace(/^- (.+)$/gm, '<li>$1</li>');
  html = html.replace(/((?:<li>.*<\/li>\n?)+)/g, '<ul>$1</ul>');
  html = html.replace(/^\d+\. (.+)$/gm, '<li>$1</li>');
  html = html.replace(/^(?!<[a-z])((?!<).+)$/gm, '<p>$1</p>');
  html = html.replace(/<p><\/p>/g, '');
  html = html.replace(/<!-- sep -->/g, '');
  return html;
}

function escHtml(s: string): string {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}
