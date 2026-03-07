import React from 'react';
import * as DropdownMenu from '@radix-ui/react-dropdown-menu';
import { FiMoreVertical } from 'react-icons/fi';
import * as styles from './ActionMenu.module.scss';

export interface ActionMenuItem {
  label: string;
  icon?: React.ReactNode;
  onClick: () => void;
  variant?: 'default' | 'danger';
  disabled?: boolean;
}

interface ActionMenuProps {
  items: ActionMenuItem[];
  triggerIcon?: React.ReactNode;
  side?: 'left' | 'right' | 'top' | 'bottom';
}

export const ActionMenu: React.FC<ActionMenuProps> = ({ items, triggerIcon, side = 'left' }) => (
  <DropdownMenu.Root>
    <DropdownMenu.Trigger asChild>
      <button className={styles.trigger}>{triggerIcon ?? <FiMoreVertical size={16} />}</button>
    </DropdownMenu.Trigger>
    <DropdownMenu.Portal>
      <DropdownMenu.Content side={side} sideOffset={6} className={styles.content} style={{ minWidth: 150 }}>
        {items.map((item, i) => {
          const isDanger = item.variant === 'danger';
          if (isDanger && i > 0) {
            return (
              <React.Fragment key={i}>
                <DropdownMenu.Separator className={styles.sep} />
                <DropdownMenu.Item
                  className={`${styles.item} ${styles.itemDanger}`}
                  onSelect={item.onClick}
                  disabled={item.disabled}
                >
                  <span className={styles.itemInner}>{item.icon}{item.label}</span>
                </DropdownMenu.Item>
              </React.Fragment>
            );
          }
          return (
            <DropdownMenu.Item key={i} className={styles.item} onSelect={item.onClick} disabled={item.disabled}>
              <span className={styles.itemInner}>{item.icon}{item.label}</span>
            </DropdownMenu.Item>
          );
        })}
      </DropdownMenu.Content>
    </DropdownMenu.Portal>
  </DropdownMenu.Root>
);
