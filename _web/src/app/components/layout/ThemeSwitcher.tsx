import React from 'react';
import { useThemeContext } from '../../contexts/ThemeContext';
import * as styles from './Sidebar.module.scss';
import { FiSun, FiMoon } from 'react-icons/fi';

export const ThemeSwitcher = () => {
    const { theme, toggleTheme } = useThemeContext();

    return (
        <button onClick={toggleTheme} className={styles.themeSwitcherButton}>
            {theme.variant === 'light' ? <FiMoon /> : <FiSun />}
            <span>{theme.variant === 'light' ? 'Dark Mode' : 'Light Mode'}</span>
        </button>
    );
};