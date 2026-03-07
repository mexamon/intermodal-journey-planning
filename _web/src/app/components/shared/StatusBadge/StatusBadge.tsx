import React from 'react';
import * as styles from './StatusBadge.module.scss';

export type BadgeVariant = 'active' | 'inactive' | 'draft' | 'deprecated' | 'warning' | 'info';

const VARIANT_CONFIG: Record<BadgeVariant, { label: string; color: string; bg: string }> = {
  active:     { label: 'Active',     color: '#22c55e', bg: 'rgba(34,197,94,0.12)' },
  inactive:   { label: 'Inactive',   color: '#ef4444', bg: 'rgba(239,68,68,0.12)' },
  draft:      { label: 'Draft',      color: '#60a5fa', bg: 'rgba(59,130,246,0.15)' },
  deprecated: { label: 'Deprecated', color: '#9ca3af', bg: 'rgba(107,114,128,0.15)' },
  warning:    { label: 'Warning',    color: '#f59e0b', bg: 'rgba(245,158,11,0.12)' },
  info:       { label: 'Info',       color: '#3b82f6', bg: 'rgba(59,130,246,0.12)' },
};

interface StatusBadgeProps {
  status: BadgeVariant | boolean;
  label?: string;
  showDot?: boolean;
  className?: string;
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status, label, showDot = true, className }) => {
  const variant: BadgeVariant = typeof status === 'boolean' ? (status ? 'active' : 'inactive') : status;
  const config = VARIANT_CONFIG[variant];
  const displayLabel = label ?? config.label;

  return (
    <span
      className={`${styles.badge} ${className ?? ''}`}
      style={{ backgroundColor: config.bg, color: config.color }}
    >
      {showDot && <span className={styles.dot} style={{ backgroundColor: config.color }} />}
      {displayLabel}
    </span>
  );
};
