import React from 'react';
import * as DropdownMenu from '@radix-ui/react-dropdown-menu';
import { FiCheck, FiChevronDown } from 'react-icons/fi';
import classNames from 'classnames';
import * as styles from './SelectField.module.scss';

export type SelectOption = {
  value: string;
  label: string;
  description?: string;
};

type AlignOption = 'start' | 'center' | 'end';

interface SelectFieldProps {
  value: string;
  options: SelectOption[];
  onChange: (value: string) => void;
  placeholder?: string;
  className?: string;
  menuClassName?: string;
  align?: AlignOption;
  disabled?: boolean;
  ariaLabel?: string;
}

export const SelectField: React.FC<SelectFieldProps> = ({
  value,
  options,
  onChange,
  placeholder,
  className,
  menuClassName,
  align = 'start',
  disabled = false,
  ariaLabel,
}) => {
  const selected = options.find((option) => option.value === value);
  const displayLabel = selected?.label ?? placeholder ?? 'Select';

  return (
    <DropdownMenu.Root>
      <DropdownMenu.Trigger asChild disabled={disabled}>
        <button
          type="button"
          className={classNames(styles.trigger, className)}
          aria-label={ariaLabel ?? displayLabel}
        >
          <span className={styles.triggerLabel}>{displayLabel}</span>
          <FiChevronDown className={styles.triggerIcon} />
        </button>
      </DropdownMenu.Trigger>
      <DropdownMenu.Portal>
        <DropdownMenu.Content
          align={align}
          sideOffset={6}
          className={classNames(styles.content, menuClassName)}
        >
          {options.map((option) => {
            const isSelected = option.value === value;
            return (
              <DropdownMenu.Item
                key={option.value}
                className={classNames(styles.item, isSelected && styles.itemSelected)}
                onSelect={() => onChange(option.value)}
              >
                <div className={styles.itemText}>
                  <span className={styles.itemLabel}>{option.label}</span>
                  {option.description && (
                    <span className={styles.itemDescription}>{option.description}</span>
                  )}
                </div>
                {isSelected && <FiCheck className={styles.itemCheck} />}
              </DropdownMenu.Item>
            );
          })}
        </DropdownMenu.Content>
      </DropdownMenu.Portal>
    </DropdownMenu.Root>
  );
};
