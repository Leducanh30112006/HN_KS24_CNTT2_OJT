import { create } from 'zustand';

/**
 * Apply theme to document root (dataset.theme and .dark class)
 */
export const applyThemeToDOM = (theme) => {
  if (typeof document === 'undefined') return;
  const root = document.documentElement;
  root.dataset.theme = theme;
  if (theme === 'dark') {
    root.classList.add('dark');
  } else {
    root.classList.remove('dark');
  }
};

/**
 * Persistent theme store managing 'light' | 'dark' mode state
 */
export const useThemeStore = create((set, get) => {
  // Determine initial state from localStorage or system preference
  let initialTheme = 'light';
  if (typeof window !== 'undefined') {
    try {
      const saved = localStorage.getItem('quickshop_theme');
      if (saved === 'dark' || saved === 'light') {
        initialTheme = saved;
      } else if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
        initialTheme = 'dark';
      }
    } catch {}
    applyThemeToDOM(initialTheme);
  }

  return {
    theme: initialTheme,
    isDark: initialTheme === 'dark',

    setTheme: (newTheme) => {
      const theme = newTheme === 'dark' ? 'dark' : 'light';
      if (typeof window !== 'undefined') {
        try {
          localStorage.setItem('quickshop_theme', theme);
        } catch {}
      }
      applyThemeToDOM(theme);
      set({ theme, isDark: theme === 'dark' });
      return theme;
    },

    toggleTheme: () => {
      const current = get().theme;
      const nextTheme = current === 'dark' ? 'light' : 'dark';
      if (typeof window !== 'undefined') {
        try {
          localStorage.setItem('quickshop_theme', nextTheme);
        } catch {}
      }
      applyThemeToDOM(nextTheme);
      set({ theme: nextTheme, isDark: nextTheme === 'dark' });
      return nextTheme;
    },
  };
});

export default useThemeStore;
