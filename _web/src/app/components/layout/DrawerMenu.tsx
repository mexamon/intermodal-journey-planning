import React, { useState } from 'react';
import * as styles from './DrawerMenu.module.scss';
import * as Tooltip from '@radix-ui/react-tooltip';
import * as DropdownMenu from '@radix-ui/react-dropdown-menu';
import { useThemeContext } from '../../contexts/ThemeContext';
import { ThyLogo } from './ThyLogo';
import { Workspace } from '../../types';
import {
    FiSettings, FiSun, FiMoon, FiLogOut, FiHelpCircle, FiChevronDown,
    FiCheck, FiPlus
} from 'react-icons/fi';
import { NavSection } from '../../data/navigation';

interface DrawerMenuProps {
    isCollapsed: boolean;
    activeTab: string;
    onWorkspaceTabSelect: (tabId: string) => void;
    onNavigateToAccount: () => void;
    onLogout: () => void;
    navSections: NavSection[];
    workspaces: Workspace[];
    selectedWorkspace: Workspace;
    onWorkspaceSelect: (workspace: Workspace) => void;
}

export const DrawerMenu: React.FC<DrawerMenuProps> = (props) => {
    const { 
        isCollapsed, activeTab, onWorkspaceTabSelect, onNavigateToAccount, onLogout,
        navSections, workspaces, selectedWorkspace, onWorkspaceSelect 
    } = props;
    
    const { theme, toggleTheme } = useThemeContext();
    const [wsSearch, setWsSearch] = useState('');

    const filteredWorkspaces = workspaces.filter(ws => 
        ws.name.toLowerCase().includes(wsSearch.toLowerCase())
    );

    return (
        <aside className={`${styles.drawer} ${isCollapsed ? styles.collapsed : ''}`}>
            <div className={styles.header}>
                <ThyLogo size={28} />
                <div className={styles.headerText}>
                    <span>Intermodal</span>
                    <span className={styles.hubText}>Planner</span>
                </div>
            </div>

            <nav className={styles.drawerNav}>
                <Tooltip.Provider delayDuration={100}>
                    {navSections.map(section => (
                        <div key={section.id} className={styles.navSection}>
                            <div className={styles.sectionLabel}>{section.label}</div>
                            {section.items.map(item => {
                                const Icon = item.icon;
                                return (
                                    <Tooltip.Root key={item.id}>
                                        <Tooltip.Trigger asChild>
                                            <button
                                                className={`${styles.navButton} ${activeTab === item.id ? styles.active : ''}`}
                                                onClick={() => onWorkspaceTabSelect(item.id)}
                                            >
                                                <span className={styles.navIcon}><Icon /></span>
                                                <span className={styles.navLabel}>{item.label}</span>
                                            </button>
                                        </Tooltip.Trigger>
                                    {isCollapsed && (
                                        <Tooltip.Portal>
                                            <Tooltip.Content className={styles.tooltipContent} side="right" sideOffset={5}>
                                                {item.label}
                                            </Tooltip.Content>
                                        </Tooltip.Portal>
                                    )}
                                    </Tooltip.Root>
                                );
                            })}
                        </div>
                    ))}
                </Tooltip.Provider>
            </nav>

            <div className={styles.footer}>
                 <DropdownMenu.Root>
                    <DropdownMenu.Trigger asChild>
                         <button className={styles.avatarButton}>
                            <img src="https://github.com/mexamon.png" alt="User Avatar" />
                             <span className={styles.userName}>Engin Mahmut</span>
                        </button>
                    </DropdownMenu.Trigger>
                    <DropdownMenu.Portal>
                         <DropdownMenu.Content className={styles.dropdownContent} side="top" align="start" sideOffset={10}>
                            <DropdownMenu.Item className={styles.dropdownItem} onSelect={onNavigateToAccount}>
                                <div className={styles.dropdownItemContent}>
                                    <FiSettings size={14} />
                                    <span>Account Settings</span>
                                </div>
                            </DropdownMenu.Item>
                            <DropdownMenu.Item className={`${styles.dropdownItem} ${styles.danger}`} onSelect={onLogout}>
                                <div className={styles.dropdownItemContent}>
                                    <FiLogOut size={14} />
                                    <span>Log Out</span>
                                </div>
                            </DropdownMenu.Item>
                        </DropdownMenu.Content>
                    </DropdownMenu.Portal>
                </DropdownMenu.Root>
                <div className={styles.footerActions}>
                    <button className={styles.footerIconButton} onClick={toggleTheme}>
                        {theme.variant === 'light' ? <FiMoon /> : <FiSun />}
                    </button>
                     <button className={styles.footerIconButton}>
                        <FiHelpCircle />
                    </button>
                </div>
            </div>
        </aside>
    );
};
