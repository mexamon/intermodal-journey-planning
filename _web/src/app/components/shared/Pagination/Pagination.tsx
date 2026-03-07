import React from 'react';
import * as styles from './Pagination.module.scss';
import * as DropdownMenu from '@radix-ui/react-dropdown-menu';
import {
  FiChevronLeft, FiChevronRight, FiChevronsLeft, FiChevronsRight, FiChevronDown, FiCheck,
} from 'react-icons/fi';

interface PaginationProps {
  currentPage: number;
  totalItems: number;
  pageSize: number;
  pageSizes?: readonly number[];
  onPageChange: (page: number) => void;
  onPageSizeChange?: (size: number) => void;
}

export const Pagination: React.FC<PaginationProps> = ({
  currentPage, totalItems, pageSize, pageSizes = [10, 20, 50],
  onPageChange, onPageSizeChange,
}) => {
  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));
  const safePage = Math.min(currentPage, totalPages - 1);
  const from = totalItems === 0 ? 0 : safePage * pageSize + 1;
  const to = Math.min((safePage + 1) * pageSize, totalItems);

  const goPage = (p: number) => onPageChange(Math.max(0, Math.min(p, totalPages - 1)));

  return (
    <div className={styles.bar}>
      <div className={styles.left}>
        <span className={styles.info}>
          {totalItems === 0 ? 'No results' : `${from}–${to} of ${totalItems}`}
        </span>
      </div>
      <div className={styles.center}>
        <button className={styles.btn} disabled={safePage === 0} onClick={() => goPage(0)} title="First">
          <FiChevronsLeft size={14} />
        </button>
        <button className={styles.btn} disabled={safePage === 0} onClick={() => goPage(safePage - 1)} title="Previous">
          <FiChevronLeft size={14} />
        </button>
        <span className={styles.info} style={{ minWidth: 80, textAlign: 'center' }}>
          Page {safePage + 1} of {totalPages}
        </span>
        <button className={styles.btn} disabled={safePage >= totalPages - 1} onClick={() => goPage(safePage + 1)} title="Next">
          <FiChevronRight size={14} />
        </button>
        <button className={styles.btn} disabled={safePage >= totalPages - 1} onClick={() => goPage(totalPages - 1)} title="Last">
          <FiChevronsRight size={14} />
        </button>
      </div>
      <div className={styles.right}>
        {onPageSizeChange && (
          <DropdownMenu.Root>
            <DropdownMenu.Trigger asChild>
              <button className={styles.sizeBtn}>
                {pageSize} / page <FiChevronDown size={12} />
              </button>
            </DropdownMenu.Trigger>
            <DropdownMenu.Portal>
              <DropdownMenu.Content sideOffset={4} align="end" side="top" className={styles.dropdown} style={{ minWidth: 100 }}>
                {pageSizes.map(sz => (
                  <DropdownMenu.Item key={sz} className={styles.dropdownItem} onSelect={() => { onPageSizeChange(sz); onPageChange(0); }}>
                    {sz} / page
                    {pageSize === sz && <FiCheck size={14} />}
                  </DropdownMenu.Item>
                ))}
              </DropdownMenu.Content>
            </DropdownMenu.Portal>
          </DropdownMenu.Root>
        )}
      </div>
    </div>
  );
};
