/**
 * Cryptographically strong random helpers backed by Web Crypto API.
 */
export function secureRandomInt(minInclusive, maxExclusive) {
  const range = Math.max(1, Math.floor(maxExclusive - minInclusive));
  const array = new Uint32Array(1);
  globalThis.crypto.getRandomValues(array);
  return minInclusive + (array[0] % range);
}

export function secureRandomToken(length = 8) {
  const bytes = new Uint8Array(Math.ceil(length));
  globalThis.crypto.getRandomValues(bytes);
  return Array.from(bytes, (b) => (b % 36).toString(36)).join('').slice(0, length);
}
