import { defaultProducts } from '../data/sampleProducts.js';

// Precomputed fast lookup maps from sample products to guarantee 100% correct typography
const skuVariantMap = new Map();
const variantIdMap = new Map();
const productNameMap = new Map();

for (const p of defaultProducts) {
  if (p.name) {
    productNameMap.set(p.name.trim().toLowerCase(), p);
  }
  if (Array.isArray(p.variants)) {
    for (const v of p.variants) {
      if (v.sku) {
        skuVariantMap.set(String(v.sku).trim().toUpperCase(), { color: v.color, size: v.size });
      }
      if (v.id) {
        variantIdMap.set(String(v.id), { color: v.color, size: v.size });
      }
    }
  }
}

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

// Direct dictionary patterns for corrupt Vietnamese tokens that arise from common DB encodings
const directDictionary = [
  [/Trá[°º]¯ng\s+Sang\s+Trá[»º][\x8d\x80-\x9f\s]*ng/gi, 'Trắng Sang Trọng'],
  [/Trá[°º]¯ng/gi, 'Trắng'],
  [/Bá[º°]£n\s+TiÃªu\s+Chuá[º°]©n/gi, 'Bản Tiêu Chuẩn'],
  [/KÃ¨m\s+Bá»™\s+BÆ¡m\s+Xáº£\s+Tá»±\s+Ä[ \x90]á»™ng/gi, 'Kèm Bộ Bơm Xả Tự Động'],
  [/KÃ¨m\s+Bá»™/gi, 'Kèm Bộ'],
  [/BÆ¡m\s+Xáº£/gi, 'Bơm Xả'],
  [/Tá»±\s+Ä[ \x90]á»™ng/gi, 'Tự Động'],
  [/Bá[º°]£n\s+Quá»[‘\x91]c\s+Táº¿/gi, 'Bản Quốc Tế'],
  [/ChÃ[­\xad]nh\s+HÃ£ng/gi, 'Chính Hãng'],
  [/Ä[ \x90]en\s+NhÃ¡m/gi, 'Đen Nhám'],
  [/Ä[ \x90]en/gi, 'Đen'],
];

/**
 * Checks if a string contains double-encoded UTF-8 / Mojibake patterns
 */
export function isMojibake(str) {
  if (!str || typeof str !== 'string') return false;
  return /(?:[\xC2-\xDF][\x80-\xBF]|[\xE0-\xEF][\x80-\xBF]{2}|[\xC0-\xFF][\x80-\xFF]|Ã[\x80-\xFF]|Ä[\x80-\xFF\s]|Æ[\x80-\xFF]|á[º»°±\x80-\xbf]|Tá»|Giáº|KhÃ|BÃ|cÆ|sÃ|Trá[°º»\x80-\xbf]|Bá[º»°\x80-\xbf]|Ä\u0090|[™…–—\u0080-\u009F]|\uFFFD)/.test(str);
}

/**
 * Automatically decodes Mojibake text into proper UTF-8 Vietnamese
 */
export function fixMojibakeText(str) {
  if (!str || typeof str !== 'string') return str;
  if (!isMojibake(str)) return str;

  // 1. Direct dictionary pass for known corrupt patterns
  let s = str;
  for (const [pattern, replacement] of directDictionary) {
    if (pattern.test(s)) {
      s = s.replace(pattern, replacement);
    }
  }

  // 2. Pre-process surrogate markers like degree symbol '°' (0xB0) mistakenly replacing 'º' (0xBA)
  const prepared = s
    .replace(/á°/g, 'áº')
    .replace(/á±/g, 'á»');

  // 3. Algorithmic CP1252 / ISO-8859-1 byte array reconstruction to UTF-8
  try {
    const bytes = [];
    for (let i = 0; i < prepared.length; i++) {
      const ch = prepared[i];
      if (cp1252Map[ch] !== undefined) {
        bytes.push(cp1252Map[ch]);
      } else {
        const code = prepared.charCodeAt(i);
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
    // Return dictionary-cleaned string on decode error
  }

  return s;
}

/**
 * Normalizes a product object by restoring clean Vietnamese from defaultProducts
 * and automatically decoding any server Mojibake.
 */
export function normalizeProduct(product) {
  if (!product) return product;

  // Search by ID, SKU, or name
  let sampleMatch = defaultProducts.find((p) => String(p.id) === String(product.id));

  if (!sampleMatch && product.variants && product.variants.length > 0) {
    for (const v of product.variants) {
      if (v.sku) {
        sampleMatch = defaultProducts.find((p) => p.variants?.some((sv) => String(sv.sku).toUpperCase() === String(v.sku).toUpperCase()));
        if (sampleMatch) break;
      }
    }
  }

  if (!sampleMatch && product.name) {
    const lowerName = product.name.trim().toLowerCase();
    sampleMatch = productNameMap.get(lowerName) || defaultProducts.find((p) => lowerName.includes(p.name.slice(0, 20).toLowerCase()));
  }

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
    // 1. Try SKU direct match
    const skuMatch = v.sku ? skuVariantMap.get(String(v.sku).trim().toUpperCase()) : null;
    // 2. Try variant ID direct match
    const idMatch = v.id ? variantIdMap.get(String(v.id)) : null;
    // 3. Try sampleMatch variant
    const sampleVar = sampleMatch?.variants?.find((sv) => sv.sku === v.sku || String(sv.id) === String(v.id)) || sampleMatch?.variants?.[idx];

    const cleanColor = skuMatch?.color || idMatch?.color || sampleVar?.color || fixMojibakeText(v.color) || 'Tiêu chuẩn';
    const cleanSize = skuMatch?.size || idMatch?.size || sampleVar?.size || fixMojibakeText(v.size) || 'Tiêu chuẩn';

    const variantName = v.variantName ? fixMojibakeText(v.variantName) : ([cleanColor, cleanSize].filter(Boolean).join(' - ') || 'Mặc định');

    return {
      ...sampleVar,
      ...v,
      color: cleanColor,
      size: cleanSize,
      variantName,
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
