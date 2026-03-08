import React from 'react';
import { Drawer } from 'vaul';
import { FiX } from 'react-icons/fi';
import * as styles from './VaulDrawer.module.scss';

/* ═══════════════════════════════════════════════
   VaulDrawer — Reusable right-side slide-in drawer
   Uses SCSS module for theme-aware styling (.dark/.light)
   ═══════════════════════════════════════════════ */
export interface VaulDrawerProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title?: string;
  width?: number | string;
  children: React.ReactNode;
  footer?: React.ReactNode;
}

export const VaulDrawer: React.FC<VaulDrawerProps> = ({
  open, onOpenChange, title, width = 520, children, footer,
}) => {
  return (
    <Drawer.Root
      open={open}
      onOpenChange={onOpenChange}
      direction="right"
      handleOnly={true}
      modal={true}
    >
      <Drawer.Portal>
        <Drawer.Overlay className={styles.overlay} onClick={() => onOpenChange(false)} />
        <Drawer.Content
          className={styles.drawerContent}
          style={{ width: typeof width === 'number' ? `${width}px` : width }}
          data-vaul-no-drag
        >
          <div className={styles.panel}>
            {/* Header */}
            {title && (
              <div className={styles.header}>
                <Drawer.Title className={styles.title}>{title}</Drawer.Title>
                <button className={styles.closeBtn} onClick={() => onOpenChange(false)}>
                  <FiX size={18} />
                </button>
              </div>
            )}

            {/* Scrollable body — no-drag so dropdowns/inputs work */}
            <div className={styles.body} data-vaul-no-drag>
              {children}
            </div>

            {/* Footer */}
            {footer && (
              <div className={styles.footer}>
                {footer}
              </div>
            )}
          </div>
        </Drawer.Content>
      </Drawer.Portal>
    </Drawer.Root>
  );
};
