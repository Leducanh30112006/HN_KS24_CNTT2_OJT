import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App.jsx';
import './index.css';
import { FALLBACK_PRODUCT_IMAGE } from './utils/imageFallback';

// Global capture-phase fallback cho mọi thẻ <img> bị lỗi mạng / chặn ảnh bên thứ 3
if (typeof window !== 'undefined') {
  // Lọc cảnh báo deprecation THREE.Clock từ thư viện @react-three/fiber r168+
  const origWarn = console.warn;
  console.warn = (...args) => {
    if (typeof args[0] === 'string' && (
      args[0].includes('THREE.Clock: This module has been deprecated') ||
      args[0].includes('Invalid scope')
    )) {
      return;
    }
    origWarn.apply(console, args);
  };

  // Global defensive fallback cho shopProfile để loại trừ hoàn toàn ReferenceError
  if (!window.shopProfile) {
    window.shopProfile = {
      shopId: 1,
      shopName: 'TechWorld Official Store',
      ownerName: 'Lê Đức Anh',
    };
  }

  window.addEventListener(
    'error',
    (event) => {
      const target = event?.target;
      if (target && target.tagName === 'IMG' && target.dataset?.fallbackApplied !== '1') {
        target.dataset.fallbackApplied = '1';
        target.src = FALLBACK_PRODUCT_IMAGE;
      }
    },
    true
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
