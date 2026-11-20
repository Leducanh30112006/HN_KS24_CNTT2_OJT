import React, { useState, useEffect, useCallback } from 'react';
import { ArrowUp } from 'lucide-react';

/**
 * ScrollToTopButton
 * High-craft floating button with circular SVG scroll-depth progress ring,
 * smooth Lenis / native scroll integration, and tactile hover/press physics.
 */
export default function ScrollToTopButton({ threshold = 280 }) {
  const [isVisible, setIsVisible] = useState(false);
  const [scrollProgress, setScrollProgress] = useState(0);

  const handleScroll = useCallback(() => {
    const currentScrollY = window.scrollY || document.documentElement.scrollTop;
    setIsVisible(currentScrollY > threshold);

    const docHeight = document.documentElement.scrollHeight - window.innerHeight;
    if (docHeight > 0) {
      const progress = Math.min(100, Math.max(0, (currentScrollY / docHeight) * 100));
      setScrollProgress(progress);
    }
  }, [threshold]);

  useEffect(() => {
    window.addEventListener('scroll', handleScroll, { passive: true });
    handleScroll();
    return () => window.removeEventListener('scroll', handleScroll);
  }, [handleScroll]);

  const scrollToTop = () => {
    if (window.__lenis && typeof window.__lenis.scrollTo === 'function') {
      window.__lenis.scrollTo(0, { duration: 1.1 });
    } else {
      window.scrollTo({
        top: 0,
        behavior: 'smooth',
      });
    }
  };

  // SVG circular progress calculation
  const radius = 20;
  const circumference = 2 * Math.PI * radius;
  const strokeDashoffset = circumference - (scrollProgress / 100) * circumference;

  return (
    <div
      className={`fixed right-4 sm:right-6 bottom-20 sm:bottom-24 z-40 transition-all duration-300 ease-out ${
        isVisible
          ? 'opacity-100 translate-y-0 scale-100 pointer-events-auto'
          : 'opacity-0 translate-y-4 scale-75 pointer-events-none'
      }`}
    >
      <button
        type="button"
        onClick={scrollToTop}
        aria-label="Cuộn lên đầu trang"
        className="relative group w-12 h-12 rounded-full bg-white/95 dark:bg-[#13141C]/95 hover:bg-white dark:hover:bg-[#1A1B25] text-slate-700 dark:text-slate-200 hover:text-orange-600 dark:hover:text-amber-400 border border-slate-200/90 dark:border-white/10 shadow-lg hover:shadow-xl hover:shadow-orange-500/20 backdrop-blur-md flex items-center justify-center transition-all duration-200 hover:scale-105 active:scale-90 cursor-pointer focus:outline-none focus:ring-2 focus:ring-orange-500/30 select-none"
      >
        {/* Circular SVG Scroll Progress Ring */}
        <svg
          className="absolute inset-0 w-full h-full -rotate-90 pointer-events-none"
          viewBox="0 0 48 48"
        >
          {/* Background subtle track */}
          <circle
            cx="24"
            cy="24"
            r={radius}
            fill="none"
            stroke="currentColor"
            strokeWidth="2.5"
            className="text-slate-100 dark:text-white/10"
          />
          {/* Active progress stroke with orange-amber gradient */}
          <circle
            cx="24"
            cy="24"
            r={radius}
            fill="none"
            stroke="url(#scroll-top-gradient)"
            strokeWidth="2.5"
            strokeDasharray={circumference}
            strokeDashoffset={strokeDashoffset}
            strokeLinecap="round"
            className="transition-all duration-150 ease-out"
          />
          <defs>
            <linearGradient id="scroll-top-gradient" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#F97316" />
              <stop offset="50%" stopColor="#F43F5E" />
              <stop offset="100%" stopColor="#F59E0B" />
            </linearGradient>
          </defs>
        </svg>

        {/* Center Icon with Lift Motion on Hover */}
        <ArrowUp className="w-5 h-5 stroke-[2.5] text-slate-700 dark:text-slate-200 group-hover:text-orange-600 dark:group-hover:text-amber-400 group-hover:-translate-y-0.5 transition-all duration-200 relative z-10" />

        {/* Floating Tooltip on Hover */}
        <div className="absolute right-full mr-2.5 top-1/2 -translate-y-1/2 bg-slate-900/95 backdrop-blur-md text-white text-[11px] font-semibold px-2.5 py-1 rounded-lg shadow-xl whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none flex items-center gap-1">
          <span>Lên đầu trang</span>
          <span className="text-[9px] text-amber-300 font-mono">({Math.round(scrollProgress)}%)</span>
        </div>
      </button>
    </div>
  );
}
