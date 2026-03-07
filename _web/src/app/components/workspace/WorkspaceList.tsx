// File: src/app/components/workspace/WorkspaceList.tsx
import React from 'react';
import * as styles from './Workspace.module.scss';
import { Workspace } from '../../types';
import { WorkspaceCard } from './WorkspaceCard';

interface WorkspaceListProps {
    workspaces: Workspace[];
    onManageClick: (workspace: Workspace) => void;
}

const WorkspaceList: React.FC<WorkspaceListProps> = ({ workspaces, onManageClick }) => {
    return (
        <div className={styles.workspaceList}>
            {workspaces.map(ws => (
                <WorkspaceCard 
                    key={ws.id} 
                    workspace={ws} 
                    onManageClick={() => onManageClick(ws)} 
                />
            ))}
        </div>
    );
};

export { WorkspaceList };