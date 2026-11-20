/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ['class', '[data-theme="dark"]'],
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ['"Plus Jakarta Sans"', '-apple-system', 'BlinkMacSystemFont', '"Segoe UI"', 'Roboto', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'ui-monospace', 'SFMono-Regular', 'Menlo', 'Monaco', 'Consolas', 'monospace'],
      },
      colors: {
        obsidian: {
          DEFAULT: '#0F172A',
          deep: '#07080D',
          card: '#13141C',
          surface: '#1A1B25',
          elevated: '#252632',
        },
        alabaster: '#FAFAFA',
        brand: {
          orange: '#F97316',
          darkOrange: '#EA580C',
          amber: '#F59E0B',
          amberLight: '#FBBF24',
        },
        ux: {
          page: 'var(--ux-page)',
          surface: 'var(--ux-surface)',
          'surface-2': 'var(--ux-surface-2)',
          'surface-3': 'var(--ux-surface-3)',
          ink: 'var(--ux-ink)',
          'ink-2': 'var(--ux-ink-2)',
          'ink-3': 'var(--ux-ink-3)',
          line: 'var(--ux-line)',
          'line-2': 'var(--ux-line-2)',
          accent: 'var(--ux-accent)',
          'accent-2': 'var(--ux-accent-2)',
        },
      },
    },
  },
  plugins: [],
}
