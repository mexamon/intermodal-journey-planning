import React from 'react';
import { createRoot } from 'react-dom/client';
import App from './app/App';

// DÜZELTME: Ana stil dosyasını buraya import ederek
// tüm uygulamanın stilleri yüklemesini sağlıyoruz.
import './styles/main.scss';

const container = document.getElementById('root');

if (container) {
  const root = createRoot(container);
  root.render(
    <React.StrictMode>
      <App />
    </React.StrictMode>
  );
} else {
    console.error("Root element with id 'root' not found in the DOM.");
}