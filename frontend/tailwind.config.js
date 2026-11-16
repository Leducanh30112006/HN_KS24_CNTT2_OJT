/** @type {import('tailwindcss').Config} */
export default {
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
          card: '#111827',
          surface: '#1E293B',
        },
        alabaster: '#FAFAFA',
        brand: {
          orange: '#F97316',
          darkOrange: '#EA580C',
          amber: '#F59E0B',
        },
      },
    },
  },
  plugins: [],
}
