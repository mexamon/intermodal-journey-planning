import React from 'react';
import * as styles from './AppHeader.module.scss';
import { FiMenu, FiPlus, FiChevronRight } from 'react-icons/fi';
import { ThyLogo } from './ThyLogo';

interface AppHeaderProps {
    toggleDrawer: () => void;
    activeView: 'workspace' | 'account';
    onNavigate: (view: 'workspace' | 'account') => void;
    activeTabLabel: string;
    activeTabDescription?: string;
    primaryActionLabel?: string;
    onPrimaryAction?: () => void;
}

export const AppHeader: React.FC<AppHeaderProps> = ({
    toggleDrawer,
    activeView,
    onNavigate,
    activeTabLabel,
    activeTabDescription,
    primaryActionLabel,
    onPrimaryAction
}) => {
    return (
        <header className={styles.appHeader}>
            <div className={styles.headerLeft}>
                <button onClick={toggleDrawer} className={styles.headerButton}>
                    <FiMenu />
                </button>
                <nav className={styles.breadcrumb}>
                    {activeView === 'account' ? (
                        <>
                            {/* DÜZELTME: "Workspaces" yerine "Account" yazıldı ve tıklama özelliği kaldırıldı. */}
                            <span>Account</span>
                            <FiChevronRight size={16} />
                            <span className={styles.activeCrumb}>Account Settings</span>
                        </>
                    ) : (
                        <>
                            {/* Workspace görünümü için breadcrumb aynı kalıyor */}
                            <span>Intermodal</span>
                            <FiChevronRight size={16} />
                            <span className={styles.activeCrumb}>{activeTabLabel}</span>
                        </>
                    )}
                </nav>
                {activeView === 'workspace' && activeTabDescription && (
                    <span className={styles.subtitle}>{activeTabDescription}</span>
                )}
            </div>
            <div className={styles.headerRight}>
                {primaryActionLabel && onPrimaryAction && (
                    <button className={`${styles.actionButton} ${styles.primary}`} onClick={onPrimaryAction}>
                        <FiPlus />
                        <span>{primaryActionLabel}</span>
                    </button>
                )}
            </div>
        </header>
    );
};
