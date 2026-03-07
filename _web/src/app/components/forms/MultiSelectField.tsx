import React from 'react';
import * as DropdownMenu from '@radix-ui/react-dropdown-menu';
import { FiCheck, FiChevronDown } from 'react-icons/fi';
import classNames from 'classnames';
import * as styles from './MultiSelectField.module.scss';

export type MultiSelectOption = {
  value: string;
  label: string;
  description?: string;
};

type AlignOption = 'start' | 'center' | 'end';

interface MultiSelectFieldProps {
  values: string[];
  options: MultiSelectOption[];
  onChange: (values: string[]) => void;
  placeholder?: string;
  className?: string;
  menuClassName?: string;
  align?: AlignOption;
  disabled?: boolean;
  ariaLabel?: string;
  allLabel?: string;
}

export const MultiSelectField: React.FC<MultiSelectFieldProps> = ({
  values,
  options,
  onChange,
  placeholder,
  className,
  menuClassName,
  align = 'start',
  disabled = false,
  ariaLabel,
  allLabel = 'All',
}) => {
  const selectedLabels = options
    .filter((option) => values.includes(option.value))
    .map((option) => option.label);
  const allSelected = values.length === 0 || values.length === options.length;
  const displayLabel = allSelected
    ? (placeholder ?? allLabel)
    : selectedLabels.length <= 2
      ? selectedLabels.join(', ')
      : `${selectedLabels.length} selected`;

  const toggleValue = (value: string) => {
    const next = values.includes(value)
      ? values.filter((entry) => entry !== value)
      : [...values, value];
    const deduped = Array.from(new Set(next));
    if (deduped.length === options.length) {
      onChange([]);
    } else {
      onChange(deduped);
    }
  };

  return (
    <DropdownMenu.Root>
      <DropdownMenu.Trigger asChild disabled={disabled}>
        <button
          type="button"
          className={classNames(styles.trigger, className)}
          aria-label={ariaLabel ?? displayLabel}
        >
          <span className={styles.triggerLabel}>{displayLabel}</span>
          {!allSelected && values.length > 0 && (
            <span className={styles.count}>{values.length}</span>
          )}
          <FiChevronDown className={styles.triggerIcon} />
        </button>
      </DropdownMenu.Trigger>
      <DropdownMenu.Portal>
        <DropdownMenu.Content
          align={align}
          sideOffset={6}
          className={classNames(styles.content, menuClassName)}
        >
          <DropdownMenu.CheckboxItem
            className={styles.item}
            checked={allSelected}
            onCheckedChange={() => onChange([])}
            onSelect={(event) => event.preventDefault()}
          >
            <div className={styles.itemText}>
              <span className={styles.itemLabel}>{allLabel}</span>
              <span className={styles.itemDescription}>Reset filters</span>
            </div>
            {allSelected && <FiCheck className={styles.itemCheck} />}
          </DropdownMenu.CheckboxItem>
          <div className={styles.separator} />
          {options.map((option) => {
            const checked = values.includes(option.value);
            return (
              <DropdownMenu.CheckboxItem
                key={option.value}
                className={classNames(styles.item, checked && styles.itemSelected)}
                checked={checked}
                onCheckedChange={() => toggleValue(option.value)}
                onSelect={(event) => event.preventDefault()}
              >
                <div className={styles.itemText}>
                  <span className={styles.itemLabel}>{option.label}</span>
                  {option.description && (
                    <span className={styles.itemDescription}>{option.description}</span>
                  )}
                </div>
                {checked && <FiCheck className={styles.itemCheck} />}
              </DropdownMenu.CheckboxItem>
            );
          })}
        </DropdownMenu.Content>
      </DropdownMenu.Portal>
    </DropdownMenu.Root>
  );
};
