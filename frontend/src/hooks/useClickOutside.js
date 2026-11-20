import { useEffect, useRef } from 'react';

/**
 * useClickOutside
 * Calls handler when a pointer/mouse click occurs outside the referenced element(s).
 * Also listens for the Escape key to dismiss the popup/dropdown/modal.
 *
 * @param {React.RefObject | React.RefObject[]} ref - Single ref or array of refs to treat as "inside"
 * @param {Function} handler - Callback to invoke when clicked outside or Escape pressed
 * @param {Object} [options]
 * @param {boolean} [options.enabled=true] - Whether the listener is active
 * @param {boolean} [options.listenEscape=true] - Whether to trigger handler on Escape key
 * @param {React.RefObject[]} [options.ignoreRefs=[]] - Refs to ignore (e.g. toggle button)
 */
export function useClickOutside(ref, handler, options = {}) {
  const {
    enabled = true,
    listenEscape = true,
    ignoreRefs = [],
  } = options;

  const savedHandler = useRef(handler);
  useEffect(() => {
    savedHandler.current = handler;
  }, [handler]);

  useEffect(() => {
    if (!enabled) return;

    const handlePointerDown = (event) => {
      // Nếu click vào bên trong ref chính -> không đóng
      const refs = Array.isArray(ref) ? ref : [ref];
      for (const r of refs) {
        if (r && r.current && r.current.contains(event.target)) {
          return;
        }
      }

      // Nếu click vào nút kích hoạt (toggle button) trong ignoreRefs -> bỏ qua để nút tự quản lý toggle
      for (const ir of ignoreRefs) {
        if (ir && ir.current && ir.current.contains(event.target)) {
          return;
        }
      }

      if (savedHandler.current) {
        savedHandler.current(event);
      }
    };

    const handleKeyDown = (event) => {
      if (listenEscape && event.key === 'Escape') {
        if (savedHandler.current) {
          savedHandler.current(event);
        }
      }
    };

    document.addEventListener('mousedown', handlePointerDown);
    document.addEventListener('touchstart', handlePointerDown, { passive: true });
    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('mousedown', handlePointerDown);
      document.removeEventListener('touchstart', handlePointerDown);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [ref, enabled, listenEscape, ignoreRefs]);
}

export default useClickOutside;
