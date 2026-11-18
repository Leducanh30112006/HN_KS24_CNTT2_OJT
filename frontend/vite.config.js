import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    host: true,
    proxy: {
      '/api': {
        target: process.env.VITE_API_GATEWAY_URL || 'https://quickshop.io.vn',
        changeOrigin: true,
      },
    },
  },
  build: {
    target: 'es2022',
    rollupOptions: {
      output: {
        manualChunks: {
          ckeditor: ['ckeditor5', '@ckeditor/ckeditor5-react'],
          vendor: ['react', 'react-dom', 'react-router-dom', 'zustand', 'axios'],
        },
      },
    },
  },
});
