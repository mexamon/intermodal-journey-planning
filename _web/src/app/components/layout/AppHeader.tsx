import React, { useState, useRef, useEffect } from 'react';
import * as styles from './AppHeader.module.scss';
import { FiMenu, FiPlus, FiChevronRight, FiChevronDown } from 'react-icons/fi';
import { ThyLogo } from './ThyLogo';
import { useSettingsStore, CURRENCIES, CurrencyCode } from '../../stores/settingsStore';

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
    const { currency, setCurrency } = useSettingsStore();
    const [currencyOpen, setCurrencyOpen] = useState(false);
    const dropdownRef = useRef<HTMLDivElement>(null);

    const currentCurrency = CURRENCIES.find(c => c.code === currency) || CURRENCIES[0];

    // Close dropdown on outside click
    useEffect(() => {
        const handler = (e: MouseEvent) => {
            if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
                setCurrencyOpen(false);
            }
        };
        document.addEventListener('mousedown', handler);
        return () => document.removeEventListener('mousedown', handler);
    }, []);

    return (
        <header className={styles.appHeader}>
            <div className={styles.headerLeft}>
                <button onClick={toggleDrawer} className={styles.headerButton}>
                    <FiMenu />
                </button>
                <nav className={styles.breadcrumb}>
                    {activeView === 'account' ? (
                        <>
                            <span>Account</span>
                            <FiChevronRight size={16} />
                            <span className={styles.activeCrumb}>Account Settings</span>
                        </>
                    ) : (
                        <>
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
                {/* Currency Selector */}
                <div className={styles.currencySelector} ref={dropdownRef}>
                    <button
                        className={styles.currencyButton}
                        onClick={() => setCurrencyOpen(!currencyOpen)}
                    >
                        <span className={styles.currencySymbol}>{currentCurrency.symbol}</span>
                        <span className={styles.currencyCode}>{currentCurrency.code}</span>
                        <FiChevronDown size={14} className={currencyOpen ? styles.chevronOpen : ''} />
                    </button>
                    {currencyOpen && (
                        <div className={styles.currencyDropdown}>
                            {CURRENCIES.map(c => (
                                <button
                                    key={c.code}
                                    className={`${styles.currencyOption} ${c.code === currency ? styles.active : ''}`}
                                    onClick={() => { setCurrency(c.code); setCurrencyOpen(false); }}
                                >
                                    <span className={styles.currencySymbol}>{c.symbol}</span>
                                    <span>{c.code}</span>
                                    <span className={styles.currencyLabel}>{c.label}</span>
                                </button>
                            ))}
                        </div>
                    )}
                </div>

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
