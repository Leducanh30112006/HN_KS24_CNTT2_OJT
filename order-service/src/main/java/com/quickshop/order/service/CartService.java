package com.quickshop.order.service;

import com.quickshop.order.dto.cart.AddToCartRequest;
import com.quickshop.order.dto.cart.CartResponse;
import com.quickshop.order.dto.cart.MergeCartRequest;
import com.quickshop.order.dto.cart.MergeCartResponse;
import com.quickshop.order.dto.cart.UpdateCartItemRequest;

import java.util.List;

public interface CartService {

    /**
     * Lấy toàn bộ thông tin giỏ hàng của người dùng, tự động gom nhóm theo Shop.
     *
     * @param userId ID người dùng
     * @return CartResponse chứa danh sách shop và các item tương ứng
     */
    CartResponse getCart(Long userId);

    /**
     * Thêm sản phẩm vào giỏ hàng hoặc cộng dồn số lượng nếu đã có.
     *
     * @param userId  ID người dùng
     * @param request Thông tin SKU và số lượng cần thêm
     * @return CartResponse sau khi cập nhật
     */
    CartResponse addToCart(Long userId, AddToCartRequest request);

    /**
     * Cập nhật số lượng của một mặt hàng cụ thể trong giỏ hàng.
     * Nếu số lượng mới = 0, tự động xóa khỏi giỏ.
     *
     * @param userId    ID người dùng
     * @param variantId ID biến thể SKU
     * @param request   Số lượng mới
     * @return CartResponse sau khi cập nhật
     */
    CartResponse updateCartItemQuantity(Long userId, Long variantId, UpdateCartItemRequest request);

    /**
     * Xóa một mặt hàng khỏi giỏ hàng.
     *
     * @param userId    ID người dùng
     * @param variantId ID biến thể SKU cần xóa
     * @return CartResponse sau khi xóa
     */
    CartResponse removeCartItem(Long userId, Long variantId);

    /**
     * Làm trống toàn bộ giỏ hàng của người dùng.
     *
     * @param userId ID người dùng
     */
    void clearCart(Long userId);

    /**
     * Hợp nhất danh sách mặt hàng từ Guest Cart vào User Cart (SRS 2.2.0 - FR-05).
     *
     * @param userId  ID người dùng
     * @param request Dữ liệu Guest Cart kèm requestId chống lặp
     * @return MergeCartResponse chứa giỏ hàng đã cập nhật và danh sách cảnh báo
     */
    MergeCartResponse mergeCart(Long userId, MergeCartRequest request);

    /**
     * Xóa danh sách các mặt hàng đã checkout thành công khỏi Redis Cart.
     *
     * @param userId     ID người dùng
     * @param variantIds Danh sách ID các biến thể SKU đã thanh toán/đặt hàng
     */
    void removeCartItems(Long userId, List<Long> variantIds);
}

