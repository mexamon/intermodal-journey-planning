import React, { useMemo, useState } from 'react';
import * as styles from './HubPage.module.scss';
import { mockWorkspaces } from './data/mockData';
import { useThemeContext } from './contexts/ThemeContext';
import { useAuth } from './contexts/AuthContext';
import { DrawerMenu } from './components/layout/DrawerMenu';
import { AppHeader } from './components/layout/AppHeader';
import { HubLayout } from './components/layout/HubLayout';
import * as paneStyles from './panes/Panes.module.scss';
import {
    UsersPane, AccountSettingsPane, PlannerPane, LocationsPane, RoutesPane,
    AnalyticsPane, FlowStudioPane, ConnectionsPane, TransportModesPane, ProvidersPane,
    ServiceAreasPane, FaresPane, KnowledgeBasePane,
} from './panes';
import { navItemsFlat, navSections } from './data/navigation';

const PlaceholderPane: React.FC<{ title: string; description: string }> = ({ title, description }) => (
    <>
        <header className={paneStyles.paneHeader}>
            <h2>{title}</h2>
            <p>{description}</p>
        </header>
        <div className={paneStyles.placeholder}>
            <p>Bu modül yakında gelecek.</p>
        </div>
    </>
);

export const HubPage = () => {
    useThemeContext();
    const { logout, user } = useAuth();
    const [isDrawerCollapsed, setDrawerCollapsed] = useState(false);
    const [currentView, setCurrentView] = useState<'workspace' | 'account'>('workspace');
    const [activeTab, setActiveTab] = useState('planner');
    const [selectedWorkspace, setSelectedWorkspace] = useState(mockWorkspaces[0]);

    // Filter navigation by user role
    const filteredNavSections = useMemo(() => {
        const role = user?.role;
        return navSections
            .map(section => ({
                ...section,
                items: section.items.filter(item =>
                    !item.requiredRole || item.requiredRole === role
                ),
            }))
            .filter(section => section.items.length > 0);
    }, [user?.role]);

    const handleWorkspaceNavigation = (tabId: string) => {
        setActiveTab(tabId);
        setCurrentView('workspace');
    };

    const renderWorkspaceContent = () => {
        switch (activeTab) {
            case 'planner': return <PlannerPane />;
            case 'routes': return <RoutesPane />;
            case 'locations': return <LocationsPane />;
            case 'connections': return <ConnectionsPane />;
            case 'modes': return <TransportModesPane />;
            case 'providers': return <ProvidersPane />;
            case 'service-areas': return <ServiceAreasPane />;
            case 'fares': return <FaresPane />;
            case 'flows': return <FlowStudioPane />;
            case 'users': return <UsersPane />;
            case 'analytics': return <AnalyticsPane />;
            case 'knowledge-base': return <KnowledgeBasePane />;
            default: {
                const activeItem = navItemsFlat.find(item => item.id === activeTab);
                if (!activeItem) return null;
                return (
                    <PlaceholderPane
                        title={activeItem.label}
                        description={activeItem.description}
                    />
                );
            }
        }
    };
    
    const activeTabLabel = navItemsFlat.find(item => item.id === activeTab)?.label || '';
    const activeTabDescription = navItemsFlat.find(item => item.id === activeTab)?.description || '';

    const isFlowStudio = currentView === 'workspace' && activeTab === 'flows';

    return (
        <div className={styles.appContainer}>
            <DrawerMenu
                isCollapsed={isDrawerCollapsed}
                activeTab={activeTab}
                onWorkspaceTabSelect={handleWorkspaceNavigation}
                onNavigateToAccount={() => setCurrentView('account')}
                onLogout={logout}
                navSections={filteredNavSections}
                workspaces={mockWorkspaces}
                selectedWorkspace={selectedWorkspace}
                onWorkspaceSelect={setSelectedWorkspace}
            />
            <div className={`${styles.mainContent} ${isDrawerCollapsed ? styles.collapsed : ''}`}>
                <AppHeader 
                    toggleDrawer={() => setDrawerCollapsed(p => !p)}
                    activeTabLabel={activeTabLabel}
                    activeTabDescription={activeTabDescription}
                    activeView={currentView}
                    onNavigate={setCurrentView}
                />
                <div className={`${styles.scrollableArea} ${isFlowStudio ? styles.flowFullBleed : ''}`}>
                    {currentView === 'workspace' ? (
                        <HubLayout className={isFlowStudio ? styles.flowFullBleedLayout : ''}>
                            <main className={`${styles.contentArea} ${isFlowStudio ? styles.flowFullBleed : ''}`}>
                                {renderWorkspaceContent()}
                            </main>
                        </HubLayout>
                    ) : (
                        <AccountSettingsPane />
                    )}
                </div>
            </div>
        </div>
    );
};
