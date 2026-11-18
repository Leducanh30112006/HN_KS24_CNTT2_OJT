import DOMPurify from 'dompurify';

/**
 * Cấu hình bảo mật làm sạch mã HTML của bài viết mô tả sản phẩm.
 * Sử dụng DOMPurify để ngăn chặn tấn công XSS (Cross-Site Scripting),
 * cho phép các thẻ định dạng văn bản an toàn, bảng, danh sách, liên kết,
 * và hình ảnh tải lên từ MinIO Object Storage (img, figure, figcaption).
 */
export const sanitizeProductDescription = (dirtyHtml) => {
  if (!dirtyHtml || typeof dirtyHtml !== 'string') {
    return '';
  }

  // Khởi tạo purify instance an toàn cho cả môi trường trình duyệt và môi trường kiểm thử/SSR
  const purify = typeof DOMPurify?.sanitize === 'function'
    ? DOMPurify
    : (typeof window !== 'undefined' ? DOMPurify(window) : null);

  // Fallback an toàn nếu chạy trong môi trường không có Window/DOM
  if (!purify || typeof purify.sanitize !== 'function') {
    return dirtyHtml
      .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
      .replace(/on\w+\s*=\s*(?:['"][^'"]*['"]|[^\s>]+)/gi, '')
      .replace(/javascript:[^"'\s>]*/gi, '#');
  }

  // Hook bảo mật xử lý sau khi làm sạch các thuộc tính (Attributes)
  if (typeof purify.addHook === 'function') {
    purify.removeHook('afterSanitizeAttributes');
    purify.addHook('afterSanitizeAttributes', (node) => {
      // 1. Bảo vệ các liên kết <a href="...">
      if (node.tagName === 'A' && node.hasAttribute('href')) {
        node.setAttribute('target', '_blank');
        node.setAttribute('rel', 'noopener noreferrer nofollow');
      }

      // 2. Bảo vệ các hình ảnh <img src="...">
      if (node.tagName === 'IMG') {
        // Tự động thêm lazy loading tối ưu tốc độ tải trang
        if (!node.hasAttribute('loading')) {
          node.setAttribute('loading', 'lazy');
        }

        // Chặn hoàn toàn giao thức nguy hiểm trong src của ảnh
        const src = node.getAttribute('src') || '';
        if (src.toLowerCase().startsWith('javascript:') || src.toLowerCase().startsWith('data:text/html')) {
          node.removeAttribute('src');
        }
      }
    });
  }

  const clean = purify.sanitize(dirtyHtml, {
    ALLOWED_TAGS: [
      'p', 'br', 'strong', 'b', 'em', 'i', 'u', 's', 'strike',
      'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
      'ul', 'ol', 'li',
      'blockquote',
      'a',
      'table', 'thead', 'tbody', 'tr', 'th', 'td',
      'span', 'sub', 'sup',
      'hr',
      // Thẻ hỗ trợ ảnh nội tuyến từ CKEditor 5
      'img', 'figure', 'figcaption', 'picture', 'source'
    ],
    ALLOWED_ATTR: [
      'href', 'title', 'target', 'rel',
      'style', 'class',
      'colspan', 'rowspan', 'border', 'align',
      // Thuộc tính hình ảnh an toàn
      'src', 'alt', 'width', 'height', 'loading', 'srcset', 'sizes'
    ],
    // Quy tắc URL hợp lệ: chỉ cho phép HTTP, HTTPS, hoặc đường dẫn tương đối
    ALLOWED_URI_REGEXP: /^(?:(?:https?|mailto|tel):|\/|#)/i,
    // Tuyệt đối cấm các thẻ có khả năng thực thi script
    FORBID_TAGS: ['script', 'iframe', 'object', 'embed', 'form', 'input', 'button', 'style'],
    // Tuyệt đối cấm các event handlers độc hại
    FORBID_ATTR: [
      'onerror', 'onload', 'onclick', 'onmouseover', 'onfocus',
      'onblur', 'onchange', 'onsubmit', 'ondblclick'
    ],
    ALLOW_DATA_ATTR: false
  });

  return clean;
};

/**
 * Render nội dung HTML an toàn, tự động tương thích ngược cho cả:
 * 1. Nội dung HTML phong phú từ CKEditor 5 (bao gồm ảnh nội tuyến)
 * 2. Nội dung văn bản thuần (plain text) cũ từ dữ liệu mẫu / ban đầu
 */
export const renderSanitizedContent = (content, fallbackText = '') => {
  if (!content || typeof content !== 'string' || !content.trim()) {
    return fallbackText ? `<p>${fallbackText}</p>` : '';
  }

  // Kiểm tra nếu nội dung có chứa thẻ HTML
  const hasHtmlTag = /<[a-z][\s\S]*>/i.test(content);
  let htmlToSanitize = content;

  if (!hasHtmlTag) {
    // Chuyển đổi văn bản thuần có ngắt dòng (\n\n) thành các đoạn <p>
    htmlToSanitize = content
      .split(/\r?\n\r?\n/)
      .map((para) => `<p>${para.replace(/\r?\n/g, '<br/>')}</p>`)
      .join('');
  }

  return sanitizeProductDescription(htmlToSanitize);
};

/**
 * Loại bỏ toàn bộ thẻ HTML để lấy văn bản thuần (plain text)
 * Thích hợp dùng cho danh sách tóm tắt, bảng biểu line-clamp, meta description
 */
export const stripHtml = (html) => {
  if (!html || typeof html !== 'string') return '';
  let plain = '';
  let insideTag = false;
  for (let i = 0; i < html.length; i++) {
    const ch = html[i];
    if (ch === '<') {
      insideTag = true;
      plain += ' ';
    } else if (ch === '>') {
      insideTag = false;
    } else if (!insideTag) {
      plain += ch;
    }
  }
  return plain
    .replaceAll('&nbsp;', ' ')
    .replaceAll('&amp;', '&')
    .replaceAll('&lt;', '<')
    .replaceAll('&gt;', '>')
    .replaceAll('&quot;', '"')
    .replace(/\s+/g, ' ')
    .trim();
};
