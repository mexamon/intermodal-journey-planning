import React, { useState, useEffect, useCallback } from 'react';
import { onToast, ToastEvent, ToastType } from '../api/client';
import { FiAlertCircle, FiCheckCircle, FiInfo, FiAlertTriangle, FiX } from 'react-icons/fi';

const TOAST_STYLES: Record<ToastType, { border: string; icon: React.ReactNode; color: string }> = {
  error:   { border: '#ef4444', icon: <FiAlertCircle size={18} />,    color: '#ef4444' },
  warning: { border: '#f59e0b', icon: <FiAlertTriangle size={18} />,  color: '#f59e0b' },
  success: { border: '#22c55e', icon: <FiCheckCircle size={18} />,    color: '#22c55e' },
  info:    { border: '#3b82f6', icon: <FiInfo size={18} />,            color: '#3b82f6' },
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
      position: 'fixed', bottom: 16, right: 16, zIndex: 99999,
      display: 'flex', flexDirection: 'column', gap: 8,
      maxWidth: 420, pointerEvents: 'none',
    }}>
      {toasts.map(toast => {
        const s = TOAST_STYLES[toast.type];
        return (
          <div key={toast.id} className="toast-item" style={{
            pointerEvents: 'auto',
            display: 'flex', alignItems: 'flex-start', gap: 10,
            padding: '12px 16px',
            borderLeft: `3px solid ${s.border}`,
            borderRadius: 8,
            boxShadow: '0 4px 20px rgba(0,0,0,0.18)',
            animation: 'toast-slide-in 0.3s ease-out',
            fontSize: 13, fontWeight: 500,
            lineHeight: 1.5,
          }}>
            <span style={{ color: s.color, flexShrink: 0, marginTop: 1 }}>{s.icon}</span>
            <span style={{ flex: 1 }}>{toast.message}</span>
            <button onClick={() => dismiss(toast.id)} style={{
              background: 'none', border: 'none', cursor: 'pointer',
              padding: 0, flexShrink: 0, marginTop: 1,
            }} className="toast-dismiss"><FiX size={14} /></button>
          </div>
        );
      })}
      <style>{`
        @keyframes toast-slide-in {
          from { opacity: 0; transform: translateX(30px); }
          to { opacity: 1; transform: translateX(0); }
        }
        .toast-item {
          background: rgba(255,255,255,0.97);
          color: #1e293b;
        }
        .toast-dismiss {
          color: #94a3b8;
        }
        @media (prefers-color-scheme: dark) {
          .toast-item {
            background: rgba(30,41,59,0.97);
            color: #e2e8f0;
          }
          .toast-dismiss {
            color: #64748b;
          }
        }
      `}</style>
    </div>
  );
};
