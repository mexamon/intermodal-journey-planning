// File: src/app/components/workspace/WorkspaceSearchBar.tsx
import React from 'react';
import { FiSearch } from 'react-icons/fi';
import * as styles from './Workspace.module.scss';

interface WorkspaceSearchBarProps {
    searchTerm: string;
    setSearchTerm: (term: string) => void;
}

const WorkspaceSearchBar: React.FC<WorkspaceSearchBarProps> = ({ searchTerm, setSearchTerm }) => {
    return (
        <div className={styles.searchContainer}>
            <FiSearch className={styles.searchIcon} />
            <input 
                type="text" 
                placeholder="Search workspaces..." 
                className={styles.searchInput}
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
            />
        </div>
    );
};

export { WorkspaceSearchBar };