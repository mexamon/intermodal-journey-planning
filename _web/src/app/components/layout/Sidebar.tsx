// File: src/app/components/layout/Sidebar.tsx
import React from 'react';
import * as styles from './Sidebar.module.scss';
import {
  FiActivity, FiGitBranch, FiCpu, FiTool, FiLayers,
  FiDatabase, FiCheckCircle, FiEye, FiUsers, FiShield
} from 'react-icons/fi';
import { ThemeSwitcher } from './ThemeSwitcher'; // Eklendi

interface SidebarProps {
  activeTab: string;
  setActiveTab: (tab: string) => void;
  currentPlan: string;
}

export const Sidebar: React.FC<SidebarProps> = ({ activeTab, setActiveTab, currentPlan }) => {
  const navItems = [
    { id: 'flows', label: 'Flow Studio', icon: <FiGitBranch /> },
    { id: 'orchestrator', label: 'Orchestrator', icon: <FiActivity /> },
    { id: 'agents', label: 'Agents', icon: <FiCpu /> },
    { id: 'tools', label: 'Tool Registry', icon: <FiTool /> },
    { id: 'models', label: 'Models', icon: <FiLayers /> },
    { id: 'knowledge', label: 'Knowledge (RAG/GraphRAG)', icon: <FiDatabase /> },
    { id: 'approvals', label: 'Approvals', icon: <FiCheckCircle /> },
    { id: 'runs', label: 'Runs & Audit', icon: <FiEye /> },
    { id: 'users', label: 'Users & Roles', icon: <FiUsers /> },
    { id: 'security', label: 'Security & Compliance', icon: <FiShield /> },
  ];

  return (
    <aside className={styles.sidebar}>
      <nav className={styles.nav}>
        {navItems.map(item => (
          <button
            key={item.id}
            className={`${styles.navButton} ${activeTab === item.id ? styles.active : ''}`}
            onClick={() => setActiveTab(item.id)}
          >
            {item.icon} {item.label}
          </button>
        ))}
      </nav>
      {/* Footer alanı için bir sarmalayıcı eklendi */}
      <div className={styles.sidebarFooter}>
        <div className={styles.planInfo}>
          <p className={styles.planTitle}>Current Plan: <strong>{currentPlan}</strong></p>
          <p className={styles.planDesc}>You are on the free plan. Upgrade for more features.</p>
          <button className={styles.upgradeButton} onClick={() => setActiveTab('flows')}>
            Go to Flow Studio
          </button>
        </div>
        <ThemeSwitcher />
      </div>
    </aside>
  );
};
