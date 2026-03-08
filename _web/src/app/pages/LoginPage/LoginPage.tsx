import React, { useState } from 'react';
import * as styles from './LoginPage.module.scss';
import { useAuth } from '../../contexts/AuthContext';
import { ThyLogo } from '../../components/layout/ThyLogo';
import { FiMail, FiLock, FiArrowRight, FiAlertCircle } from 'react-icons/fi';
import { MdFlight } from 'react-icons/md';
import { useThemeContext } from '../../contexts/ThemeContext';

export const LoginPage: React.FC = () => {
  const { login } = useAuth();
  const { theme } = useThemeContext();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim() || !password.trim()) { setError('Please fill in all fields'); return; }
    setError('');
    setLoading(true);
    const success = await login(email.trim(), password.trim());
    setLoading(false);
    if (!success) setError('Invalid email or password');
  };

  return (
    <div className={styles.loginPage}>
      {/* Background decoration */}
      <div className={styles.bgOrb1} />
      <div className={styles.bgOrb2} />

      <div className={styles.loginCard}>
        {/* Branding */}
        <div className={styles.branding}>
          <ThyLogo size={42} />
          <div className={styles.brandText}>
            <h1>Intermodal Planner</h1>
            <span>Journey Management Platform</span>
          </div>
        </div>

        <p className={styles.subtitle}>Sign in to your workspace</p>

        {/* Login Form */}
        <form onSubmit={handleSubmit} className={styles.form}>
          {error && (
            <div className={styles.errorBanner}>
              <FiAlertCircle size={15} />
              <span>{error}</span>
            </div>
          )}

          <div className={styles.field}>
            <label>Email</label>
            <div className={styles.inputWrap}>
              <FiMail size={15} className={styles.inputIcon} />
              <input
                type="email"
                placeholder="admin@thy.com"
                value={email}
                onChange={e => setEmail(e.target.value)}
                autoFocus
                autoComplete="email"
              />
            </div>
          </div>

          <div className={styles.field}>
            <label>Password</label>
            <div className={styles.inputWrap}>
              <FiLock size={15} className={styles.inputIcon} />
              <input
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={e => setPassword(e.target.value)}
                autoComplete="current-password"
              />
            </div>
          </div>

          <button type="submit" className={styles.submitBtn} disabled={loading}>
            {loading ? (
              <span className={styles.spinner} />
            ) : (
              <>Sign In <FiArrowRight size={16} /></>
            )}
          </button>
        </form>

        {/* Demo Hint */}
        <div className={styles.demoHint}>
          <MdFlight size={14} style={{ opacity: 0.5 }} />
          <span>Admin: <strong>admin@thy.com</strong> / <strong>admin</strong></span>
        </div>
        <div className={styles.demoHint}>
          <MdFlight size={14} style={{ opacity: 0.5 }} />
          <span>Agency: <strong>agency@thy.com</strong> / <strong>agency</strong></span>
        </div>
      </div>
    </div>
  );
};
