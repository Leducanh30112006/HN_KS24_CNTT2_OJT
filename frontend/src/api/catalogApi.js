import axiosClient from './axiosClient';

/**
 * Catalog API Service
 * Kết nối với backend Catalog Service qua API Gateway (/api/catalog)
 */

// Chuyển đổi mã sắp xếp từ giao diện người dùng sang chuẩn Catalog Service
export const mapSortOption = (sortBy) => {
  switch (sortBy) {
    case 'priceAsc':
      return 'price_asc';
    case 'priceDesc':
      return 'price_desc';
    case 'popular':
      return 'popular';
    case 'newest':
    default:
      return 'newest';
  }
};

// Chuyển đổi bộ lọc khoảng giá từ giao diện người dùng sang minPrice & maxPrice
export const mapPriceRange = (rangeId) => {
  switch (rangeId) {
    case 'under1m':
      return { minPrice: null, maxPrice: 1000000 };
    case '1m-5m':
      return { minPrice: 1000000, maxPrice: 5000000 };
    case '5m-20m':
      return { minPrice: 5000000, maxPrice: 20000000 };
    case 'above20m':
      return { minPrice: 20000000, maxPrice: null };
    case 'all':
    default:
      return { minPrice: null, maxPrice: null };
  }
};

export const catalogApi = {
  /**
   * Lấy danh sách sản phẩm có tìm kiếm, lọc đa tiêu chí và phân trang
   * @param {Object} params
   * @param {string} [params.keyword]
   * @param {number} [params.categoryId]
   * @param {number} [params.shopId]
   * @param {string} [params.status] (Mặc định không truyền để nhận cả APPROVED & ACTIVE)
   * @param {number} [params.minPrice]
   * @param {number} [params.maxPrice]
   * @param {number} [params.minRating]
   * @param {number} [params.page=0]
   * @param {number} [params.size=12]
   * @param {string} [params.sortBy='newest']
   * @returns {Promise<any>}
   */
  async getProducts(params = {}) {
    const queryParams = new URLSearchParams();

    if (params.keyword && params.keyword.trim()) {
      queryParams.append('keyword', params.keyword.trim());
    }
    if (params.categoryId) {
      queryParams.append('categoryId', params.categoryId);
    }
    if (params.shopId) {
      queryParams.append('shopId', params.shopId);
    }
    if (params.status) {
      queryParams.append('status', params.status);
    }
    if (params.minPrice !== null && params.minPrice !== undefined) {
      queryParams.append('minPrice', params.minPrice);
    }
    if (params.maxPrice !== null && params.maxPrice !== undefined) {
      queryParams.append('maxPrice', params.maxPrice);
    }
    if (params.minRating && params.minRating > 0) {
      queryParams.append('minRating', params.minRating);
    }

    queryParams.append('page', params.page ?? 0);
    queryParams.append('size', params.size ?? 12);
    queryParams.append('sortBy', mapSortOption(params.sortBy));

    const url = `/api/catalog/products?${queryParams.toString()}`;
    return await axiosClient.get(url);
  },

  /**
   * Lấy chi tiết sản phẩm theo ID (kèm các biến thể SKU, giá min/max, tổng tồn kho và rating)
   * @param {number|string} productId
   * @returns {Promise<any>}
   */
  async getProductById(productId) {
    return await axiosClient.get(`/api/catalog/products/${productId}`);
  },

  /**
   * Lấy danh sách toàn bộ danh mục sản phẩm sàn QuickShop
   * @returns {Promise<any>}
   */
  async getCategories() {
    return await axiosClient.get('/api/catalog/categories');
  },

  /**
   * Lấy cây danh mục phân cấp Cha - Con
   * @returns {Promise<any>}
   */
  async getCategoryTree() {
    return await axiosClient.get('/api/catalog/categories/tree');
  },

  /**
   * Lấy danh sách đánh giá của sản phẩm
   * @param {number|string} productId
   * @returns {Promise<any>}
   */
  async getProductReviews(productId) {
    return await axiosClient.get(`/api/catalog/products/${productId}/reviews`);
  },

  /**
   * Gửi đánh giá mới cho sản phẩm (1-5 sao và bình luận)
   * @param {number|string} productId
   * @param {Object} reviewData
   * @returns {Promise<any>}
   */
  async createProductReview(productId, reviewData) {
    return await axiosClient.post(`/api/catalog/products/${productId}/reviews`, reviewData);
  },
};

export default catalogApi;
