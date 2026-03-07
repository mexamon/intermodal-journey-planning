import React from 'react';
import * as styles from './HubLayout.module.scss';

interface HubLayoutProps {
    children: React.ReactNode;
    className?: string;
}

const HubLayout: React.FC<HubLayoutProps> = ({ children, className }) => {
    // Flex ve gap kaldırıldı, artık sadece bir çerçeve görevi görüyor.
    return (
        <div className={`${styles.mainContentLayout} ${className ?? ''}`}>
            {children}
        </div>
    );
};

export { HubLayout };
