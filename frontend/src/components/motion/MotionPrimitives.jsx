import React, { useState, useEffect, useRef, useLayoutEffect } from 'react';
import { gsap, ScrollTrigger } from '../../lib/gsap';

/**
 * AnimatedPresence
 * Prevents abrupt React unmounting when closing modals, drawers, and dropdowns.
 * Runs a smooth GSAP exit animation before finally removing the DOM element.
 */
export function AnimatedPresence({
  show,
  children,
  mode = 'modal', // 'modal' | 'dropdown' | 'drawer' | 'fade'
  className = '',
  backdropClassName = '',
  style = {},
  onExitComplete,
  dataLenisPrevent = true,
}) {
  const [shouldRender, setShouldRender] = useState(show);
  const containerRef = useRef(null);
  const backdropRef = useRef(null);
  const contentRef = useRef(null);
  const isFirstRender = useRef(true);

  useEffect(() => {
    if (show) {
      setShouldRender(true);
    }
  }, [show]);

  // Khóa cuộn nền của body khi mở Modal hoặc Drawer để không hiện thanh cuộn kép
  useEffect(() => {
    if (show && (mode === 'modal' || mode === 'drawer')) {
      const prevOverflow = document.body.style.overflow;
      document.body.style.overflow = 'hidden';
      return () => {
        document.body.style.overflow = prevOverflow;
      };
    }
  }, [show, mode]);

  useLayoutEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false;
      if (!show) return;
    }

    if (show && shouldRender) {
      // ENTER ANIMATION
      if (mode === 'modal') {
        if (backdropRef.current) {
          gsap.fromTo(
            backdropRef.current,
            { opacity: 0 },
            { opacity: 1, duration: 0.28, ease: 'power2.out' }
          );
        }
        if (contentRef.current) {
          gsap.fromTo(
            contentRef.current,
            { opacity: 0, scale: 0.94, y: 16 },
            {
              opacity: 1,
              scale: 1,
              y: 0,
              duration: 0.38,
              ease: 'back.out(1.4)',
              clearProps: 'transform',
            }
          );
        }
      } else if (mode === 'dropdown') {
        if (contentRef.current) {
          gsap.fromTo(
            contentRef.current,
            { opacity: 0, scale: 0.96, y: -8 },
            {
              opacity: 1,
              scale: 1,
              y: 0,
              duration: 0.24,
              ease: 'power2.out',
              clearProps: 'transform',
            }
          );
        }
      } else if (mode === 'drawer') {
        if (backdropRef.current) {
          gsap.fromTo(
            backdropRef.current,
            { opacity: 0 },
            { opacity: 1, duration: 0.25, ease: 'power2.out' }
          );
        }
        if (contentRef.current) {
          gsap.fromTo(
            contentRef.current,
            { x: '100%', opacity: 0.5 },
            { x: '0%', opacity: 1, duration: 0.35, ease: 'power3.out', clearProps: 'transform' }
          );
        }
      } else {
        // 'fade'
        if (contentRef.current) {
          gsap.fromTo(
            contentRef.current,
            { opacity: 0, y: 10 },
            { opacity: 1, y: 0, duration: 0.25, ease: 'power2.out', clearProps: 'transform' }
          );
        }
      }
    } else if (!show && shouldRender) {
      // EXIT ANIMATION
      const tl = gsap.timeline({
        onComplete: () => {
          setShouldRender(false);
          if (onExitComplete) onExitComplete();
        },
      });

      if (mode === 'modal') {
        if (contentRef.current) {
          tl.to(
            contentRef.current,
            {
              scale: 0.95,
              y: 12,
              opacity: 0,
              duration: 0.22,
              ease: 'power2.in',
            },
            0
          );
        }
        if (backdropRef.current) {
          tl.to(
            backdropRef.current,
            { opacity: 0, duration: 0.2, ease: 'power2.in' },
            0.04
          );
        }
      } else if (mode === 'dropdown') {
        if (contentRef.current) {
          tl.to(
            contentRef.current,
            {
              scale: 0.96,
              y: -6,
              opacity: 0,
              duration: 0.18,
              ease: 'power2.in',
            },
            0
          );
        }
      } else if (mode === 'drawer') {
        if (contentRef.current) {
          tl.to(
            contentRef.current,
            { x: '100%', opacity: 0.3, duration: 0.25, ease: 'power3.in' },
            0
          );
        }
        if (backdropRef.current) {
          tl.to(
            backdropRef.current,
            { opacity: 0, duration: 0.22, ease: 'power2.in' },
            0.03
          );
        }
      } else {
        // 'fade'
        if (contentRef.current) {
          tl.to(
            contentRef.current,
            { opacity: 0, y: 8, duration: 0.18, ease: 'power2.in' },
            0
          );
        }
      }
    }
  }, [show, shouldRender, mode]);

  if (!shouldRender) return null;

  // For modal / drawer with backdrop
  if (mode === 'modal' || mode === 'drawer') {
    return (
      <div
        ref={containerRef}
        {...(dataLenisPrevent ? { 'data-lenis-prevent': '' } : {})}
        className={`fixed inset-0 z-50 flex items-center justify-center ${className}`}
      >
        <div
          ref={backdropRef}
          className={`fixed inset-0 bg-black/60 backdrop-blur-xs ${backdropClassName}`}
          aria-hidden="true"
        />
        <div
          ref={contentRef}
          className="relative z-10 w-full flex flex-col items-center justify-center max-h-screen"
        >
          {children}
        </div>
      </div>
    );
  }

  // Dropdown or standalone container
  return (
    <div
      ref={contentRef}
      style={style}
      {...(dataLenisPrevent ? { 'data-lenis-prevent': '' } : {})}
      className={className}
    >
      {children}
    </div>
  );
}

/**
 * PageTransition
 * Universal View & Tab Switcher with a sleek top brand progress bar and content entrance
 */
export function PageTransition({
  viewKey,
  children,
  className = '',
  duration = 0.5,
  ease = 'expo.out',
}) {
  const containerRef = useRef(null);
  const progressBarRef = useRef(null);

  useLayoutEffect(() => {
    if (!containerRef.current) return;

    // Trigger top progress sweep
    if (progressBarRef.current) {
      gsap.fromTo(
        progressBarRef.current,
        { scaleX: 0, opacity: 1 },
        {
          scaleX: 1,
          duration: 0.45,
          ease: 'power2.out',
          onComplete: () => {
            gsap.to(progressBarRef.current, { opacity: 0, duration: 0.2 });
          },
        }
      );
    }

    // Smooth content reveal
    gsap.fromTo(
      containerRef.current,
      { opacity: 0, y: 20 },
      {
        opacity: 1,
        y: 0,
        duration: duration,
        ease: ease,
        clearProps: 'transform',
      }
    );
  }, [viewKey]);

  return (
    <div className="relative w-full">
      {/* Top brand indicator progress line */}
      <div
        ref={progressBarRef}
        className="fixed top-0 left-0 right-0 h-[2.5px] z-50 bg-gradient-to-r from-orange-500 via-amber-400 to-orange-600 origin-left pointer-events-none opacity-0"
      />
      <div ref={containerRef} className={className}>
        {children}
      </div>
    </div>
  );
}

/**
 * StaggerReveal
 * Automatically staggers child cards/rows whenever items mount, filter, or paginate
 */
export function StaggerReveal({
  triggerKey,
  selector = '.stagger-item, .product-card-item, .category-card-item, tr',
  className = '',
  stagger = 0.045,
  duration = 0.5,
  ease = 'power3.out',
  children,
}) {
  const containerRef = useRef(null);

  useLayoutEffect(() => {
    if (!containerRef.current) return;

    const items = containerRef.current.querySelectorAll(selector);
    if (items && items.length > 0) {
      gsap.fromTo(
        items,
        { opacity: 0, y: 24 },
        {
          opacity: 1,
          y: 0,
          duration: duration,
          stagger: stagger,
          ease: ease,
          clearProps: 'transform',
        }
      );
    }

    // Refresh ScrollTrigger heights after DOM measurement
    if (typeof ScrollTrigger !== 'undefined' && ScrollTrigger.refresh) {
      ScrollTrigger.refresh();
    }
  }, [triggerKey]);

  return (
    <div ref={containerRef} className={className}>
      {children}
    </div>
  );
}

/**
 * AnimatedNumber
 * Smoothly tweens numbers with GSAP for prices, cart counts, fees, and stats
 */
export function AnimatedNumber({
  value = 0,
  formatter,
  className = '',
  duration = 0.45,
  suffix = '',
  prefix = '',
}) {
  const [displayValue, setDisplayValue] = useState(Number(value) || 0);
  const tweenRef = useRef({ val: Number(value) || 0 });

  useEffect(() => {
    const targetVal = Number(value) || 0;
    gsap.to(tweenRef.current, {
      val: targetVal,
      duration: duration,
      ease: 'power2.out',
      onUpdate: () => {
        setDisplayValue(tweenRef.current.val);
      },
    });
  }, [value, duration]);

  const formatted = formatter
    ? formatter(displayValue)
    : Math.round(displayValue).toLocaleString('vi-VN');

  return (
    <span className={className}>
      {prefix}
      {formatted}
      {suffix}
    </span>
  );
}

/**
 * useMagneticHover
 * Custom hook for tactile ±4px magnetic cursor tracking
 */
export function useMagneticHover(ref, { maxDistance = 4, multiplier = 0.18, duration = 0.35 } = {}) {
  useEffect(() => {
    const el = ref?.current;
    if (!el) return;

    const xTo = gsap.quickTo(el, 'x', { duration, ease: 'power3.out' });
    const yTo = gsap.quickTo(el, 'y', { duration, ease: 'power3.out' });

    const onMouseMove = (e) => {
      const rect = el.getBoundingClientRect();
      const x = (e.clientX - (rect.left + rect.width / 2)) * multiplier;
      const y = (e.clientY - (rect.top + rect.height / 2)) * multiplier;
      xTo(Math.max(-maxDistance, Math.min(maxDistance, x)));
      yTo(Math.max(-maxDistance, Math.min(maxDistance, y)));
    };

    const onMouseLeave = () => {
      xTo(0);
      yTo(0);
    };

    el.addEventListener('mousemove', onMouseMove);
    el.addEventListener('mouseleave', onMouseLeave);

    return () => {
      el.removeEventListener('mousemove', onMouseMove);
      el.removeEventListener('mouseleave', onMouseLeave);
      xTo(0);
      yTo(0);
    };
  }, [ref, maxDistance, multiplier, duration]);
}

export default {
  AnimatedPresence,
  PageTransition,
  StaggerReveal,
  AnimatedNumber,
  useMagneticHover,
};
