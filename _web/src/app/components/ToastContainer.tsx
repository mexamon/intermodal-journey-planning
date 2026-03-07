import React, { useState, useEffect, useCallback } from 'react';
import { onToast, ToastEvent, ToastType } from '../api/client';
import { FiAlertCircle, FiCheckCircle, FiInfo, FiAlertTriangle, FiX } from 'react-icons/fi';

const TOAST_STYLES: Record<ToastType, { bg: string; border: string; icon: React.ReactNode; color: string }> = {
  error:   { bg: 'rgba(239,68,68,0.12)',  border: '#ef4444', icon: <FiAlertCircle size={18} />,    color: '#ef4444' },
  warning: { bg: 'rgba(245,158,11,0.12)', border: '#f59e0b', icon: <FiAlertTriangle size={18} />,  color: '#f59e0b' },
  success: { bg: 'rgba(34,197,94,0.12)',   border: '#22c55e', icon: <FiCheckCircle size={18} />,    color: '#22c55e' },
  info:    { bg: 'rgba(59,130,246,0.12)',  border: '#3b82f6', icon: <FiInfo size={18} />,            color: '#3b82f6' },
};

export const ToastContainer: React.FC = () => {
  const [toasts, setToasts] = useState<ToastEvent[]>([]);

  useEffect(() => {
    const unsub = onToast((toast) => {
      setToasts(prev => [...prev, toast]);
    });
    return unsub;
  }, []);

  const dismiss = useCallback((id: string) => {
    setToasts(prev => prev.filter(t => t.id !== id));
  }, []);

  useEffect(() => {
    if (toasts.length === 0) return;
    const latest = toasts[toasts.length - 1];
    const timer = setTimeout(() => dismiss(latest.id), latest.duration || 5000);
    return () => clearTimeout(timer);
  }, [toasts, dismiss]);

  if (toasts.length === 0) return null;

  return (
    <div style={{
      position: 'fixed', top: 16, right: 16, zIndex: 99999,
      display: 'flex', flexDirection: 'column', gap: 8,
      maxWidth: 420, pointerEvents: 'none',
    }}>
      {toasts.map(toast => {
        const s = TOAST_STYLES[toast.type];
        return (
          <div key={toast.id} style={{
            pointerEvents: 'auto',
            display: 'flex', alignItems: 'flex-start', gap: 10,
            padding: '12px 16px',
            background: s.bg, borderLeft: `3px solid ${s.border}`,
            borderRadius: 8,
            backdropFilter: 'blur(12px)',
            boxShadow: '0 4px 20px rgba(0,0,0,0.15)',
            animation: 'toast-slide-in 0.3s ease-out',
            fontSize: 13, fontWeight: 500, color: '#e2e8f0',
            lineHeight: 1.5,
          }}>
            <span style={{ color: s.color, flexShrink: 0, marginTop: 1 }}>{s.icon}</span>
            <span style={{ flex: 1 }}>{toast.message}</span>
            <button onClick={() => dismiss(toast.id)} style={{
              background: 'none', border: 'none', cursor: 'pointer',
              color: '#94a3b8', padding: 0, flexShrink: 0, marginTop: 1,
            }}><FiX size={14} /></button>
          </div>
        );
      })}
      <style>{`
        @keyframes toast-slide-in {
          from { opacity: 0; transform: translateX(30px); }
          to { opacity: 1; transform: translateX(0); }
        }
      `}</style>
    </div>
  );
};
