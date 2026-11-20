import React, { useRef } from 'react';
import { useThemeStore } from '../../stores/useThemeStore';

/**
 * ThemeToggle
 * Lucian Labs #219 Circular Reveal Theme Switcher
 * - Spring-driven Sun / Moon SVG icon rotation (cubic-bezier(.3, 1.4, .5, 1))
 * - Viewport-aware circular clip-path expansion via document.startViewTransition()
 * - Full accessibility with role="switch" and aria-checked
 */
export default function ThemeToggle({ className = '', size = 'md' }) {
  const buttonRef = useRef(null);
  const { isDark, toggleTheme } = useThemeStore();

  const handleToggle = () => {
    const btn = buttonRef.current;
    const prefersReducedMotion =
      typeof window !== 'undefined' &&
      window.matchMedia &&
      window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    // Graceful fallback for browsers without View Transitions API or users preferring reduced motion
    if (!document.startViewTransition || prefersReducedMotion || !btn) {
      toggleTheme();
      return;
    }

    const rect = btn.getBoundingClientRect();
    const cx = Math.round(rect.left + rect.width / 2);
    const cy = Math.round(rect.top + rect.height / 2);

    const right = window.innerWidth - cx;
    const bottom = window.innerHeight - cy;
    const maxRadius = Math.ceil(Math.hypot(Math.max(cx, right), Math.max(cy, bottom)));

    const transition = document.startViewTransition(() => {
      toggleTheme();
    });

    transition.ready
      .then(() => {
        document.documentElement.animate(
          {
            clipPath: [
              `circle(0px at ${cx}px ${cy}px)`,
              `circle(${maxRadius}px at ${cx}px ${cy}px)`,
            ],
          },
          {
            duration: 650,
            easing: 'cubic-bezier(.65, 0, .35, 1)',
            pseudoElement: '::view-transition-new(root)',
          }
        );
      })
      .catch(() => {
        // Fallback gracefully if transition fails
      });
  };

  const isSmall = size === 'sm';

  return (
    <button
      ref={buttonRef}
      type="button"
      role="switch"
      aria-checked={isDark}
      aria-label={isDark ? 'Chuyển sang giao diện Sáng' : 'Chuyển sang giao diện Tối (Dark Mode)'}
      onClick={handleToggle}
      className={`relative group inline-flex items-center justify-center rounded-2xl transition-all duration-300 active:scale-90 cursor-pointer select-none focus:outline-none focus:ring-2 focus:ring-orange-500/40 ${
        isSmall ? 'w-9 h-9 p-1.5' : 'w-10 h-10 p-2'
      } bg-slate-100 hover:bg-slate-200/90 dark:bg-white/10 dark:hover:bg-white/15 border border-slate-200/90 dark:border-white/10 text-slate-700 dark:text-amber-300 shadow-xs hover:shadow-md ${className}`}
      title={isDark ? 'Giao diện Tối (Bấm để đổi sang Sáng)' : 'Giao diện Sáng (Bấm để đổi sang Tối)'}
    >
      {/* Outer ambient glow in dark mode */}
      <span className="pointer-events-none absolute inset-0 rounded-2xl bg-amber-400/0 dark:group-hover:bg-amber-400/10 transition-colors duration-300" />

      {/* Spring Rotating Icon Container */}
      <div
        className="relative w-full h-full flex items-center justify-center transition-transform duration-500"
        style={{
          transform: isDark ? 'rotate(180deg)' : 'rotate(0deg)',
          transitionTimingFunction: 'cubic-bezier(.3, 1.4, .5, 1)',
        }}
      >
        {/* SUN ICON (Visible in Light Mode) */}
        <svg
          className={`absolute transition-all duration-300 ${
            isDark ? 'opacity-0 scale-50 rotate-90' : 'opacity-100 scale-100 rotate-0'
          } ${isSmall ? 'w-4 h-4' : 'w-5 h-5'} stroke-[2.2] text-amber-600 fill-amber-500/20`}
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <circle cx="12" cy="12" r="4" />
          <path d="M12 2v2" />
          <path d="M12 20v2" />
          <path d="m4.93 4.93 1.41 1.41" />
          <path d="m17.66 17.66 1.41 1.41" />
          <path d="M2 12h2" />
          <path d="M20 12h2" />
          <path d="m6.34 17.66-1.41 1.41" />
          <path d="m19.07 4.93-1.41 1.41" />
        </svg>

        {/* MOON ICON (Visible in Dark Mode) */}
        <svg
          className={`absolute transition-all duration-300 ${
            isDark ? 'opacity-100 scale-100 rotate-0' : 'opacity-0 scale-50 -rotate-90'
          } ${isSmall ? 'w-4 h-4' : 'w-5 h-5'} stroke-[2.2] text-amber-300 fill-amber-400/30`}
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z" />
          <path d="M19 3v4" className="stroke-[1.8] text-amber-400" />
          <path d="M21 5h-4" className="stroke-[1.8] text-amber-400" />
        </svg>
      </div>
    </button>
  );
}
