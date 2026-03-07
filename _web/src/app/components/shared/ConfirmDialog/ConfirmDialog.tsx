import React from 'react';
import * as Dialog from '@radix-ui/react-dialog';
import { FiX, FiAlertTriangle } from 'react-icons/fi';
import * as styles from './ConfirmDialog.module.scss';

interface ConfirmDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  variant?: 'danger' | 'default';
  onConfirm: () => void;
  children?: React.ReactNode;
}

export const ConfirmDialog: React.FC<ConfirmDialogProps> = ({
  open, onOpenChange, title, description,
  confirmLabel = 'Confirm', cancelLabel = 'Cancel',
  variant = 'default', onConfirm, children,
}) => (
  <Dialog.Root open={open} onOpenChange={onOpenChange}>
    <Dialog.Portal>
      <Dialog.Overlay className={styles.overlay} />
      <Dialog.Content className={styles.content}>
        <div className={styles.header}>
          {variant === 'danger' && (
            <span className={styles.dangerIcon}><FiAlertTriangle size={18} /></span>
          )}
          <Dialog.Title className={styles.title}>{title}</Dialog.Title>
          <Dialog.Close asChild>
            <button className={styles.closeBtn}><FiX size={18} /></button>
          </Dialog.Close>
        </div>
        {description && <Dialog.Description className={styles.description}>{description}</Dialog.Description>}
        {children}
        <div className={styles.actions}>
          <Dialog.Close asChild>
            <button className={styles.cancelBtn}>{cancelLabel}</button>
          </Dialog.Close>
          <button
            className={`${styles.confirmBtn} ${variant === 'danger' ? styles.confirmDanger : ''}`}
            onClick={() => { onConfirm(); onOpenChange(false); }}
          >
            {confirmLabel}
          </button>
        </div>
      </Dialog.Content>
    </Dialog.Portal>
  </Dialog.Root>
);
