import React from 'react';

interface LoadingSpinnerProps {
  /** Size in pixels (default: 32) */
  size?: number;
  /** Text to show below the spinner */
  message?: string;
  /** Whether to overlay full parent (default: false) */
  overlay?: boolean;
}

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({
  size = 32,
  message,
  overlay = false,
}) => {
  const spinner = (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      gap: 12,
      padding: overlay ? 0 : '2rem 0',
    }}>
      <svg
        width={size}
        height={size}
        viewBox="0 0 24 24"
        fill="none"
        style={{ animation: 'loading-rotate 1s linear infinite' }}
      >
        <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3" opacity="0.15" />
        <path
          d="M12 2a10 10 0 0 1 10 10"
          stroke="var(--loading-accent, #c8102e)"
          strokeWidth="3"
          strokeLinecap="round"
          style={{ animation: 'loading-dash 1.4s ease-in-out infinite' }}
        />
      </svg>
      {message && (
        <span style={{
          fontSize: '0.82rem',
          fontWeight: 500,
          opacity: 0.6,
          letterSpacing: '0.01em',
        }}>{message}</span>
      )}
      <style>{`
        @keyframes loading-rotate {
          100% { transform: rotate(360deg); }
        }
        @keyframes loading-dash {
          0% { stroke-dasharray: 1, 62; stroke-dashoffset: 0; }
          50% { stroke-dasharray: 40, 62; stroke-dashoffset: -16; }
          100% { stroke-dasharray: 1, 62; stroke-dashoffset: -62; }
        }
      `}</style>
    </div>
  );

  if (!overlay) return spinner;

  return (
    <div style={{
      position: 'absolute',
      inset: 0,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'rgba(0,0,0,0.06)',
      backdropFilter: 'blur(2px)',
      borderRadius: 'inherit',
      zIndex: 20,
    }}>
      {spinner}
    </div>
  );
};
