import { defaultProducts } from '../data/sampleProducts.js';

// Windows-1252 / CP1252 byte-to-character mapping table for double-encoded UTF-8 repair
const cp1252Map = {
  '\u20AC': 0x80, '\u201A': 0x82, '\u0192': 0x83, '\u201E': 0x84,
  '\u2026': 0x85, '\u2020': 0x86, '\u2021': 0x87, '\u02C6': 0x88,
  '\u2030': 0x89, '\u0160': 0x8A, '\u2039': 0x8B, '\u0152': 0x8C,
  '\u017D': 0x8E, '\u2018': 0x91, '\u2019': 0x92, '\u201C': 0x93,
  '\u201D': 0x94, '\u2022': 0x95, '\u2013': 0x96, '\u2014': 0x97,
  '\u02DC': 0x98, '\u2122': 0x99, '\u0161': 0x9A, '\u203A': 0x9B,
  '\u0153': 0x9C, '\u017E': 0x9E, '\u0178': 0x9F,
};

/**
 * Checks if a string contains double-encoded UTF-8 / Mojibake patterns
 */
export function isMojibake(str) {
  if (!str || typeof str !== 'string') return false;
  return /(?:[\xC2-\xDF][\x80-\xBF]|[\xE0-\xEF][\x80-\xBF]{2}|[\xC0-\xFF][\x80-\xFF]|Ã[\x80-\xFF]|Ä[\x80-\xFF\s]|Æ[\x80-\xFF]|á[º»°±\x80-\xbf]|Tá»|Giáº|KhÃ|BÃ|cÆ|sÃ|Trá[°º»\x80-\xbf]|Bá[º»\x80-\xbf]|Ä\u0090|[™…–—\u0080-\u009F])/.test(str);
}

/**
 * Automatically decodes Mojibake text into proper UTF-8 Vietnamese
 */
export function fixMojibakeText(str) {
  if (!str || typeof str !== 'string') return str;
  if (!isMojibake(str)) return str;

  try {
    const bytes = [];
    for (let i = 0; i < str.length; i++) {
      const ch = str[i];
      if (cp1252Map[ch] !== undefined) {
        bytes.push(cp1252Map[ch]);
      } else {
        const code = str.charCodeAt(i);
        if (code <= 0xFF) {
          bytes.push(code);
        } else {
          const enc = new TextEncoder().encode(ch);
          for (let j = 0; j < enc.length; j++) {
            bytes.push(enc[j]);
          }
        }
      }
    }
    const decoder = new TextDecoder('utf-8', { fatal: false });
    const decoded = decoder.decode(new Uint8Array(bytes));
    if (decoded && !decoded.includes('\uFFFD') && decoded.length > 0) {
      return decoded;
    }
  } catch (e) {
    // Return original on error
  }

  return str;
}

/**
 * Normalizes a product object by restoring clean Vietnamese from defaultProducts
 * and automatically decoding any server Mojibake.
 */
export function normalizeProduct(product) {
  if (!product) return product;

  const sampleMatch = defaultProducts.find((p) => String(p.id) === String(product.id));

  // Determine cleanest name
  let name = sampleMatch?.name || fixMojibakeText(product.name);

  // Determine cleanest description
  let description = sampleMatch?.description || fixMojibakeText(product.description);

  // Determine cleanest categoryName and brand
  const categoryName = sampleMatch?.categoryName || fixMojibakeText(product.categoryName || '');
  const brand = sampleMatch?.brand || fixMojibakeText(product.brand || '');

  // Normalize variants with clean colors and sizes
  const rawVariants = (product.variants && product.variants.length > 0)
    ? product.variants
    : (sampleMatch?.variants || []);

  const variants = rawVariants.map((v, idx) => {
    const sampleVar = sampleMatch?.variants?.find((sv) => sv.sku === v.sku || String(sv.id) === String(v.id)) || sampleMatch?.variants?.[idx];

    const color = sampleVar?.color || fixMojibakeText(v.color) || 'Tiêu chuẩn';
    const size = sampleVar?.size || fixMojibakeText(v.size) || 'Tiêu chuẩn';

    return {
      ...sampleVar,
      ...v,
      color,
      size,
    };
  });

  return {
    ...sampleMatch,
    ...product,
    name,
    description,
    categoryName,
    brand,
    variants,
    highlights: (sampleMatch?.highlights && sampleMatch.highlights.length > 0)
      ? sampleMatch.highlights
      : (product.highlights ? product.highlights.map(h => fixMojibakeText(h)) : []),
    specifications: (sampleMatch?.specifications && sampleMatch.specifications.length > 0)
      ? sampleMatch.specifications
      : (product.specifications ? product.specifications.map(s => ({
          label: fixMojibakeText(s.label),
          value: fixMojibakeText(s.value),
        })) : []),
    descriptionImages: (sampleMatch?.descriptionImages && sampleMatch.descriptionImages.length > 0)
      ? sampleMatch.descriptionImages
      : (product.descriptionImages || []),
  };
}

/**
 * Normalizes an array of products
 */
export function normalizeProductList(products) {
  if (!Array.isArray(products)) return [];
  return products.map(normalizeProduct);
}

export default {
  isMojibake,
  fixMojibakeText,
  normalizeProduct,
  normalizeProductList,
};
