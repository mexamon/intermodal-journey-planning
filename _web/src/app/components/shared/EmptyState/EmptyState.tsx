import React from 'react';
import * as styles from './EmptyState.module.scss';

interface EmptyStateProps {
  icon?: React.ReactNode;
  title?: string;
  description?: string;
  action?: React.ReactNode;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon, title = 'No results', description, action,
}) => (
  <div className={styles.wrap}>
    {icon && <div className={styles.icon}>{icon}</div>}
    <h4 className={styles.title}>{title}</h4>
    {description && <p className={styles.desc}>{description}</p>}
    {action && <div className={styles.action}>{action}</div>}
  </div>
);
