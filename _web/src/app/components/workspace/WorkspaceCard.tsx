// File: src/app/components/workspace/WorkspaceCard.tsx
import React from 'react';
import * as styles from './Workspace.module.scss';
import { Workspace } from '../../types';

interface WorkspaceCardProps {
    workspace: Workspace;
    onManageClick: () => void;
}

const WorkspaceCard: React.FC<WorkspaceCardProps> = ({ workspace, onManageClick }) => {
    return (
        <div className={styles.workspaceCard}>
            <img src={workspace.logoUrl} alt={`${workspace.name} logo`} className={styles.workspaceLogo} />
            <div className={styles.workspaceInfo}>
                <h4>{workspace.name}</h4>
                <p>{workspace.ownerEmail}</p>
                <span>Current plan: <strong>{workspace.plan}</strong></span>
            </div>
            <button 
                className={styles.manageButton}
                onClick={onManageClick}
            >
                Manage
            </button>
        </div>
    );
};

export { WorkspaceCard };