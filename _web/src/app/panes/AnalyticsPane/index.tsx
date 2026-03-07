import React, { useState, useMemo } from 'react';
import * as paneStyles from '../Panes.module.scss';
import * as s from './AnalyticsPane.module.scss';
import {
  FiGlobe, FiMap, FiTruck, FiActivity, FiNavigation, FiClock, FiSearch,
  FiTrendingUp, FiTrendingDown, FiUsers, FiZap, FiShield, FiCheckCircle, FiAlertTriangle,
  FiRefreshCw, FiCalendar, FiChevronDown,
} from 'react-icons/fi';
import { MdFlight, MdDirectionsBus, MdTrain, MdDirectionsWalk, MdLocalTaxi, MdSubway } from 'react-icons/md';

/* ━━━━━━━━━━━ MOCK DATA ━━━━━━━━━━━ */
const KPI_CARDS = [
  { icon: <FiSearch />, label: 'Total Searches', value: '24,891', change: '+12.3%', up: true, color: '#3b82f6', sub: 'Last 30 days' },
  { icon: <FiNavigation />, label: 'Routes Found', value: '156,420', change: '+8.7%', up: true, color: '#22c55e', sub: 'Avg 6.3 per search' },
  { icon: <FiUsers />, label: 'Active Users', value: '1,247', change: '+5.2%', up: true, color: '#8b5cf6', sub: 'Across all tenants' },
  { icon: <FiZap />, label: 'Avg Response', value: '340ms', change: '-18.5%', up: false, color: '#f59e0b', sub: 'P95: 890ms' },
  { icon: <FiGlobe />, label: 'Locations', value: '12,847', change: '+2.1%', up: true, color: '#06b6d4', sub: '42 countries' },
  { icon: <FiShield />, label: 'Active Policies', value: '7', change: '0%', up: true, color: '#C8102E', sub: '3 drafts' },
];

const MODE_DISTRIBUTION = [
  { mode: 'Flight', icon: <MdFlight />, pct: 42, count: '10,454', color: '#3b82f6' },
  { mode: 'Train', icon: <MdTrain />, pct: 24, count: '5,974', color: '#22c55e' },
  { mode: 'Metro', icon: <MdSubway />, pct: 15, count: '3,734', color: '#8b5cf6' },
  { mode: 'Bus', icon: <MdDirectionsBus />, pct: 11, count: '2,738', color: '#f59e0b' },
  { mode: 'Taxi', icon: <MdLocalTaxi />, pct: 5, count: '1,245', color: '#ef4444' },
  { mode: 'Walking', icon: <MdDirectionsWalk />, pct: 3, count: '746', color: '#6b7280' },
];

const HOURLY_DATA = [12, 8, 5, 3, 2, 4, 18, 42, 68, 85, 72, 63, 78, 92, 88, 76, 82, 95, 71, 54, 38, 28, 22, 16];

const TOP_ROUTES = [
  { route: 'IST → LHR', searches: 1284, avgDuration: '14h 20m', avgCost: '€189', trend: 12 },
  { route: 'IST → FRA', searches: 942, avgDuration: '12h 45m', avgCost: '€156', trend: 8 },
  { route: 'SAW → STN', searches: 671, avgDuration: '16h 10m', avgCost: '€94', trend: -3 },
  { route: 'IST → CDG', searches: 524, avgDuration: '13h 30m', avgCost: '€172', trend: 15 },
  { route: 'IST → MUC', searches: 412, avgDuration: '11h 55m', avgCost: '€134', trend: 5 },
  { route: 'ADB → AMS', searches: 389, avgDuration: '15h 20m', avgCost: '€148', trend: -2 },
  { route: 'IST → FCO', searches: 356, avgDuration: '10h 40m', avgCost: '€128', trend: 22 },
  { route: 'ESB → VIE', searches: 298, avgDuration: '9h 15m', avgCost: '€112', trend: 7 },
];

const RECENT_ACTIVITY = [
  { type: 'search', desc: 'IST → LHR', detail: '5 routes found', user: 'admin@thy.cloud', time: '2 min ago' },
  { type: 'policy', desc: 'Updated "Default Route Policy"', detail: 'Max legs: 5→4', user: 'ops@thy.cloud', time: '15 min ago' },
  { type: 'search', desc: 'SAW → STN', detail: '3 routes found', user: 'admin@thy.cloud', time: '22 min ago' },
  { type: 'location', desc: 'Added Munich Airport (MUC)', detail: 'Auto-seeded', user: 'system', time: '1h ago' },
  { type: 'search', desc: 'IST → FRA', detail: '8 routes found', user: 'ops@thy.cloud', time: '1.5h ago' },
  { type: 'connection', desc: 'New connection IST→SZG', detail: 'Train, 18h', user: 'ops@thy.cloud', time: '2h ago' },
  { type: 'search', desc: 'IST → CDG', detail: '6 routes found', user: 'admin@thy.cloud', time: '3h ago' },
  { type: 'policy', desc: 'New "Summer Routes" policy', detail: 'Draft', user: 'admin@thy.cloud', time: '4h ago' },
];

const SYSTEM_HEALTH = [
  { label: 'API Gateway', status: 'healthy' as const, latency: '12ms', uptime: '99.98%' },
  { label: 'Route Engine', status: 'healthy' as const, latency: '340ms', uptime: '99.95%' },
  { label: 'Database', status: 'healthy' as const, latency: '8ms', uptime: '99.99%' },
  { label: 'Cache Layer', status: 'warning' as const, latency: '45ms', uptime: '99.87%' },
];

/* ━━━━━━━━━━━ MINI SPARKLINE ━━━━━━━━━━━ */
const Sparkline: React.FC<{ data: number[]; color?: string; height?: number }> = ({ data, color = '#C8102E', height = 48 }) => {
  const max = Math.max(...data);
  const min = Math.min(...data);
  const w = 100;
  const points = data.map((v, i) => `${(i / (data.length - 1)) * w},${height - ((v - min) / (max - min)) * (height - 4) - 2}`).join(' ');
  const areaPoints = `0,${height} ${points} ${w},${height}`;
  return (
    <svg viewBox={`0 0 ${w} ${height}`} className={s.sparklineSvg}>
      <defs>
        <linearGradient id="sparkGrad" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={color} stopOpacity="0.2" />
          <stop offset="100%" stopColor={color} stopOpacity="0" />
        </linearGradient>
      </defs>
      <polygon points={areaPoints} fill="url(#sparkGrad)" />
      <polyline points={points} fill="none" stroke={color} strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
};

/* ━━━━━━━━━━━ COMPONENT ━━━━━━━━━━━ */
export const AnalyticsPane: React.FC = () => {
  const [period, setPeriod] = useState<'7d' | '30d' | '90d'>('30d');
  const maxSearches = Math.max(...TOP_ROUTES.map(r => r.searches));

  const activityIcon = (type: string) => {
    switch (type) {
      case 'search': return <FiSearch size={13} />;
      case 'policy': return <FiShield size={13} />;
      case 'location': return <FiGlobe size={13} />;
      case 'connection': return <FiMap size={13} />;
      default: return <FiActivity size={13} />;
    }
  };

  return (
    <>
      {/* ── Header ── */}
      <div className={s.dashHeader}>
        <div className={s.dashTitle}>
          <FiActivity size={20} />
          <h2>Analytics Overview</h2>
        </div>
        <div className={s.periodSelector}>
          {(['7d', '30d', '90d'] as const).map(p => (
            <button key={p} className={`${s.periodBtn} ${period === p ? s.active : ''}`} onClick={() => setPeriod(p)}>
              {p === '7d' ? '7 Days' : p === '30d' ? '30 Days' : '90 Days'}
            </button>
          ))}
        </div>
      </div>

      {/* ── KPI Grid ── */}
      <div className={s.kpiGrid}>
        {KPI_CARDS.map((kpi, i) => (
          <div key={i} className={s.kpiCard}>
            <div className={s.kpiTop}>
              <div className={s.kpiIcon} style={{ color: kpi.color, backgroundColor: `${kpi.color}12` }}>{kpi.icon}</div>
              <span className={`${s.kpiChange} ${kpi.change.startsWith('-') ? s.negative : s.positive}`}>
                {kpi.change.startsWith('-') ? <FiTrendingDown size={11} /> : <FiTrendingUp size={11} />}
                {kpi.change}
              </span>
            </div>
            <div className={s.kpiValue}>{kpi.value}</div>
            <div className={s.kpiLabel}>{kpi.label}</div>
            <div className={s.kpiSub}>{kpi.sub}</div>
          </div>
        ))}
      </div>

      {/* ── Row: Sparkline + Mode Distribution ── */}
      <div className={s.twoColumn}>
        {/* Search Volume */}
        <div className={s.card}>
          <div className={s.cardHeader}>
            <h3><FiTrendingUp size={15} /> Search Volume</h3>
            <span className={s.cardBadge}>Today</span>
          </div>
          <div className={s.sparklineWrap}>
            <Sparkline data={HOURLY_DATA} height={80} />
            <div className={s.sparklineLabels}>
              <span>00:00</span><span>06:00</span><span>12:00</span><span>18:00</span><span>23:00</span>
            </div>
          </div>
          <div className={s.sparklineStats}>
            <div><strong>95</strong><span>Peak</span></div>
            <div><strong>48.2</strong><span>Average</span></div>
            <div><strong>1,157</strong><span>Total</span></div>
          </div>
        </div>

        {/* Transport Mode Distribution */}
        <div className={s.card}>
          <div className={s.cardHeader}>
            <h3><FiTruck size={15} /> Mode Distribution</h3>
          </div>
          <div className={s.modeList}>
            {MODE_DISTRIBUTION.map((m, i) => (
              <div key={i} className={s.modeRow}>
                <div className={s.modeInfo}>
                  <span className={s.modeIcon} style={{ color: m.color }}>{m.icon}</span>
                  <span className={s.modeName}>{m.mode}</span>
                  <span className={s.modeCount}>{m.count}</span>
                </div>
                <div className={s.modeBarWrap}>
                  <div className={s.modeBar} style={{ width: `${m.pct}%`, backgroundColor: m.color }} />
                </div>
                <span className={s.modePct}>{m.pct}%</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* ── Row: Top Routes + Activity Feed ── */}
      <div className={s.twoColumn}>
        {/* Top Routes */}
        <div className={s.card}>
          <div className={s.cardHeader}>
            <h3><MdFlight size={15} /> Top Routes</h3>
            <span className={s.cardBadge}>Last {period === '7d' ? '7' : period === '30d' ? '30' : '90'} Days</span>
          </div>
          <div className={paneStyles.tableWrapper}>
            <table className={paneStyles.dataTable}>
              <thead>
                <tr>
                  <th>#</th>
                  <th>Route</th>
                  <th>Searches</th>
                  <th>Avg Duration</th>
                  <th>Avg Cost</th>
                  <th>Trend</th>
                </tr>
              </thead>
              <tbody>
                {TOP_ROUTES.map((r, i) => (
                  <tr key={i}>
                    <td><span className={s.rankBadge}>{i + 1}</span></td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                        <MdFlight size={13} style={{ color: '#C8102E' }} />
                        <strong>{r.route}</strong>
                      </div>
                    </td>
                    <td>
                      <div className={s.searchBarCell}>
                        <div className={s.miniBar}>
                          <div style={{ width: `${(r.searches / maxSearches) * 100}%` }} />
                        </div>
                        <span>{r.searches.toLocaleString()}</span>
                      </div>
                    </td>
                    <td>{r.avgDuration}</td>
                    <td>{r.avgCost}</td>
                    <td>
                      <span className={`${s.trendBadge} ${r.trend >= 0 ? s.positive : s.negative}`}>
                        {r.trend >= 0 ? <FiTrendingUp size={11} /> : <FiTrendingDown size={11} />}
                        {Math.abs(r.trend)}%
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Activity Feed */}
        <div className={s.card}>
          <div className={s.cardHeader}>
            <h3><FiClock size={15} /> Recent Activity</h3>
          </div>
          <div className={s.activityFeed}>
            {RECENT_ACTIVITY.map((a, i) => (
              <div key={i} className={s.activityItem}>
                <div className={s.activityIcon}>{activityIcon(a.type)}</div>
                <div className={s.activityContent}>
                  <div className={s.activityDesc}>
                    <strong>{a.desc}</strong>
                    <span>{a.detail}</span>
                  </div>
                  <div className={s.activityMeta}>
                    <span>{a.user}</span>
                    <span>{a.time}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* ── System Health ── */}
      <div className={s.card}>
        <div className={s.cardHeader}>
          <h3><FiShield size={15} /> System Health</h3>
          <button className={s.refreshBtn}><FiRefreshCw size={13} /> Refresh</button>
        </div>
        <div className={s.healthGrid}>
          {SYSTEM_HEALTH.map((h, i) => (
            <div key={i} className={`${s.healthCard} ${s[h.status]}`}>
              <div className={s.healthTop}>
                {h.status === 'healthy' ? <FiCheckCircle size={15} /> : <FiAlertTriangle size={15} />}
                <span className={s.healthLabel}>{h.label}</span>
              </div>
              <div className={s.healthStats}>
                <div><span>Latency</span><strong>{h.latency}</strong></div>
                <div><span>Uptime</span><strong>{h.uptime}</strong></div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </>
  );
};
