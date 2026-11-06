package com.quickshop.order.service;

import com.quickshop.common.exception.BadRequestException;
import com.quickshop.order.client.CatalogServiceClient;
import com.quickshop.order.dto.cart.CartItemResponse;
import com.quickshop.order.dto.cart.CartResponse;
import com.quickshop.order.dto.cart.CartShopGroupResponse;
import com.quickshop.order.dto.catalog.CatalogVariantValidationDto;
import com.quickshop.order.dto.checkout.*;
import com.quickshop.order.dto.voucher.VoucherValidationResponse;
import com.quickshop.order.entity.*;
import com.quickshop.order.repository.MasterOrderRepository;
import com.quickshop.order.repository.OrderItemRepository;
import com.quickshop.order.repository.SubOrderRepository;
import com.quickshop.order.service.impl.CheckoutServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock
    private MasterOrderRepository masterOrderRepository;

    @Mock
    private SubOrderRepository subOrderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartService cartService;

    @Mock
    private CatalogServiceClient catalogServiceClient;

    @Mock
    private VoucherService voucherService;

    @InjectMocks
    private CheckoutServiceImpl checkoutService;

    private CheckoutRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleRequest = CheckoutRequest.builder()
                .paymentMethod("COD")
                .recipientName("Nguyen Van Kien")
                .recipientPhone("0987654321")
                .shippingAddress("123 Duong Le Loi, Quan 1, TP.HCM")
                .note("Giao gio hanh chinh")
                .items(List.of(
                        CheckoutItemDto.builder().variantId(101L).quantity(2).build()
                ))
                .build();
    }

    @Test
    @DisplayName("checkout: Đơn hàng 1 Shop thanh toán COD -> tạo MasterOrder (PROCESSING) và 1 SubOrder kèm OrderItem snapshot SKU")
    void testCheckout_SingleShop_COD_Success() {
        Long userId = 1L;

        CatalogVariantValidationDto variant = CatalogVariantValidationDto.builder()
                .variantId(101L)
                .productId(201L)
                .productName("Áo thun QuickShop")
                .variantName("Đen / L")
                .sku("TSHIRT-BLK-L")
                .imageUrl("https://cdn.example.com/item1.jpg")
                .price(BigDecimal.valueOf(150000))
                .availableStock(50)
                .active(true)
                .shopId(10L)
                .shopName("Official Fashion Shop")
                .build();

        when(catalogServiceClient.getVariantDetails(101L)).thenReturn(Optional.of(variant));

        when(masterOrderRepository.save(any(MasterOrder.class))).thenAnswer(invocation -> {
            MasterOrder mo = invocation.getArgument(0);
            mo.setId(1001L);
            mo.setCreatedAt(LocalDateTime.now());
            return mo;
        });

        when(subOrderRepository.save(any(SubOrder.class))).thenAnswer(invocation -> {
            SubOrder so = invocation.getArgument(0);
            so.setId(5001L);
            so.setCreatedAt(LocalDateTime.now());
            return so;
        });

        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> {
            OrderItem oi = invocation.getArgument(0);
            oi.setId(9001L);
            return oi;
        });

        CheckoutResponse response = checkoutService.checkout(userId, sampleRequest);

        assertNotNull(response);
        assertEquals(1001L, response.getMasterOrderId());
        assertEquals("QS-101001", response.getOrderCode());
        assertEquals(MasterOrderStatus.PROCESSING, response.getStatus());
        assertEquals(BigDecimal.valueOf(300000), response.getSubtotalAmount()); // 150k * 2
        assertEquals(BigDecimal.valueOf(25000), response.getShippingFeeTotal()); // 1 shop
        assertEquals(BigDecimal.valueOf(325000), response.getTotalAmount()); // 300k + 25k
        assertEquals(1, response.getTotalShops());
        assertEquals(2, response.getTotalItems());
        assertEquals(1, response.getSubOrders().size());

        SubOrderResponse subOrderRes = response.getSubOrders().get(0);
        assertEquals(10L, subOrderRes.getShopId());
        assertEquals("Official Fashion Shop", subOrderRes.getShopName());
        assertEquals(BigDecimal.valueOf(300000), subOrderRes.getSubtotalAmount());
        assertEquals(BigDecimal.valueOf(25000), subOrderRes.getShippingFee());
        assertEquals(BigDecimal.valueOf(325000), subOrderRes.getFinalAmount());

        OrderItemResponse itemRes = subOrderRes.getItems().get(0);
        assertEquals(101L, itemRes.getVariantId());
        assertEquals("TSHIRT-BLK-L", itemRes.getSku());
        assertEquals("Áo thun QuickShop", itemRes.getProductName());
        assertEquals(BigDecimal.valueOf(150000), itemRes.getPrice());
        assertEquals(2, itemRes.getQuantity());

        // Verify cart items cleanup
        verify(cartService, times(1)).removeCartItems(eq(userId), eq(List.of(101L)));
    }

    @Test
    @DisplayName("checkout: Đơn hàng Multi-Shop (2 shops) thanh toán VNPAY -> Phí ship x2 và tạo 2 SubOrders riêng biệt")
    void testCheckout_MultiShop_OnlinePayment_Success() {
        Long userId = 2L;

        sampleRequest.setPaymentMethod("VNPAY");
        sampleRequest.setItems(List.of(
                CheckoutItemDto.builder().variantId(101L).quantity(1).build(),
                CheckoutItemDto.builder().variantId(102L).quantity(2).build()
        ));

        CatalogVariantValidationDto item1 = CatalogVariantValidationDto.builder()
                .variantId(101L)
                .productId(201L)
                .productName("Giày Thể Thao")
                .variantName("Size 42")
                .sku("SHOE-42")
                .imageUrl("https://cdn.example.com/shoe.jpg")
                .price(BigDecimal.valueOf(200000))
                .availableStock(10)
                .active(true)
                .shopId(10L)
                .shopName("Sneaker Store")
                .build();

        CatalogVariantValidationDto item2 = CatalogVariantValidationDto.builder()
                .variantId(102L)
                .productId(202L)
                .productName("Bàn Phím Cơ")
                .variantName("Blue Switch")
                .sku("KEYBOARD-BLUE")
                .imageUrl("https://cdn.example.com/kb.jpg")
                .price(BigDecimal.valueOf(300000))
                .availableStock(5)
                .active(true)
                .shopId(20L)
                .shopName("Tech Zone")
                .build();

        when(catalogServiceClient.getVariantDetails(101L)).thenReturn(Optional.of(item1));
        when(catalogServiceClient.getVariantDetails(102L)).thenReturn(Optional.of(item2));

        when(masterOrderRepository.save(any(MasterOrder.class))).thenAnswer(invocation -> {
            MasterOrder mo = invocation.getArgument(0);
            mo.setId(1002L);
            return mo;
        });

        when(subOrderRepository.save(any(SubOrder.class))).thenAnswer(invocation -> {
            SubOrder so = invocation.getArgument(0);
            so.setId((long) (Math.random() * 1000 + 1));
            return so;
        });

        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> {
            OrderItem oi = invocation.getArgument(0);
            oi.setId((long) (Math.random() * 1000 + 1));
            return oi;
        });

        CheckoutResponse response = checkoutService.checkout(userId, sampleRequest);

        assertNotNull(response);
        assertEquals(MasterOrderStatus.PENDING_PAYMENT, response.getStatus());
        assertEquals(2, response.getTotalShops());
        assertEquals(3, response.getTotalItems());
        assertEquals(BigDecimal.valueOf(800000), response.getSubtotalAmount()); // 200k*1 + 300k*2 = 800k
        assertEquals(BigDecimal.valueOf(50000), response.getShippingFeeTotal()); // 2 shops * 25k = 50k
        assertEquals(BigDecimal.valueOf(850000), response.getTotalAmount());
        assertEquals(2, response.getSubOrders().size());

        verify(cartService, times(1)).removeCartItems(eq(userId), eq(List.of(101L, 102L)));
    }

    @Test
    @DisplayName("checkout: Áp dụng Voucher giảm giá theo % (PERCENTAGE) có chặn trần tối đa")
    void testCheckout_WithVoucher_Percentage_Success() {
        Long userId = 1L;
        sampleRequest.setVoucherCode("GIAM20K");
        sampleRequest.setItems(List.of(
                CheckoutItemDto.builder().variantId(101L).quantity(1).build()
        ));

        CatalogVariantValidationDto item = CatalogVariantValidationDto.builder()
                .variantId(101L)
                .productId(201L)
                .productName("Sản phẩm A")
                .variantName("Mẫu 1")
                .sku("SKU-A")
                .imageUrl("https://cdn.example.com/a.jpg")
                .price(BigDecimal.valueOf(200000))
                .availableStock(10)
                .active(true)
                .shopId(1L)
                .shopName("Shop 1")
                .build();

        when(catalogServiceClient.getVariantDetails(101L)).thenReturn(Optional.of(item));

        when(voucherService.validateAndCalculateVoucher(eq("GIAM20K"), isNull(), any()))
                .thenReturn(VoucherValidationResponse.builder()
                        .code("GIAM20K")
                        .valid(true)
                        .discountAmount(BigDecimal.valueOf(20000))
                        .build());

        when(masterOrderRepository.save(any(MasterOrder.class))).thenAnswer(i -> {
            MasterOrder m = i.getArgument(0);
            m.setId(1003L);
            return m;
        });
        when(subOrderRepository.save(any(SubOrder.class))).thenAnswer(i -> {
            SubOrder s = i.getArgument(0);
            s.setId(2001L);
            return s;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(i -> {
            OrderItem o = i.getArgument(0);
            o.setId(3001L);
            return o;
        });

        CheckoutResponse response = checkoutService.checkout(userId, sampleRequest);

        assertEquals(BigDecimal.valueOf(20000), response.getVoucherDiscount());
        // Subtotal (200k) + Ship (25k) - Discount (20k) = 205k
        assertEquals(BigDecimal.valueOf(205000), response.getTotalAmount());
        verify(voucherService, times(1)).recordUsage(eq("GIAM20K"), eq(userId), eq(1003L), isNull(), eq(BigDecimal.valueOf(20000)));
    }

    @Test
    @DisplayName("checkout: Áp dụng Voucher số tiền cố định (FIXED_AMOUNT)")
    void testCheckout_WithVoucher_FixedAmount_Success() {
        Long userId = 1L;
        sampleRequest.setVoucherCode("FREESHIP25K");

        CatalogVariantValidationDto item = CatalogVariantValidationDto.builder()
                .variantId(101L)
                .productId(201L)
                .productName("Sản phẩm B")
                .sku("SKU-B")
                .price(BigDecimal.valueOf(100000))
                .availableStock(10)
                .active(true)
                .shopId(1L)
                .shopName("Shop 1")
                .build();

        when(catalogServiceClient.getVariantDetails(101L)).thenReturn(Optional.of(item));

        when(voucherService.validateAndCalculateVoucher(eq("FREESHIP25K"), isNull(), any()))
                .thenReturn(VoucherValidationResponse.builder()
                        .code("FREESHIP25K")
                        .valid(true)
                        .discountAmount(BigDecimal.valueOf(25000))
                        .build());

        when(masterOrderRepository.save(any(MasterOrder.class))).thenAnswer(i -> {
            MasterOrder m = i.getArgument(0);
            m.setId(1004L);
            return m;
        });
        when(subOrderRepository.save(any(SubOrder.class))).thenAnswer(i -> {
            SubOrder s = i.getArgument(0);
            s.setId(2002L);
            return s;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(i -> {
            OrderItem o = i.getArgument(0);
            o.setId(3002L);
            return o;
        });

        CheckoutResponse response = checkoutService.checkout(userId, sampleRequest);

        assertEquals(BigDecimal.valueOf(25000), response.getVoucherDiscount());
        // Subtotal (200k) + Ship (25k) - Discount (25k) = 200k
        assertEquals(BigDecimal.valueOf(200000), response.getTotalAmount());
        verify(voucherService, times(1)).recordUsage(eq("FREESHIP25K"), eq(userId), eq(1004L), isNull(), eq(BigDecimal.valueOf(25000)));
    }

    @Test
    @DisplayName("checkout: Áp dụng Voucher Kép (cả Voucher Sàn và Voucher của Shop cùng lúc)")
    void testCheckout_WithDualVoucher_PlatformAndShop_Success() {
        Long userId = 1L;
        sampleRequest.setVoucherCode("SAN30K");
        sampleRequest.setShopVouchers(Map.of(10L, "SHOP15K"));
        sampleRequest.setItems(List.of(
                CheckoutItemDto.builder().variantId(101L).quantity(1).build()
        ));

        CatalogVariantValidationDto item = CatalogVariantValidationDto.builder()
                .variantId(101L)
                .productId(201L)
                .productName("Sản phẩm Shop 10")
                .sku("SKU-S10")
                .price(BigDecimal.valueOf(200000))
                .availableStock(10)
                .active(true)
                .shopId(10L)
                .shopName("Shop 10 Official")
                .build();

        when(catalogServiceClient.getVariantDetails(101L)).thenReturn(Optional.of(item));

        // Mock Shop voucher
        when(voucherService.validateAndCalculateVoucher(eq("SHOP15K"), eq(10L), any()))
                .thenReturn(VoucherValidationResponse.builder()
                        .code("SHOP15K")
                        .valid(true)
                        .discountAmount(BigDecimal.valueOf(15000))
                        .build());

        // Mock Platform voucher
        when(voucherService.validateAndCalculateVoucher(eq("SAN30K"), isNull(), any()))
                .thenReturn(VoucherValidationResponse.builder()
                        .code("SAN30K")
                        .valid(true)
                        .discountAmount(BigDecimal.valueOf(30000))
                        .build());

        when(masterOrderRepository.save(any(MasterOrder.class))).thenAnswer(i -> {
            MasterOrder m = i.getArgument(0);
            m.setId(1005L);
            return m;
        });
        when(subOrderRepository.save(any(SubOrder.class))).thenAnswer(i -> {
            SubOrder s = i.getArgument(0);
            s.setId(2005L);
            return s;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(i -> {
            OrderItem o = i.getArgument(0);
            o.setId(3005L);
            return o;
        });

        CheckoutResponse response = checkoutService.checkout(userId, sampleRequest);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(30000), response.getVoucherDiscount()); // Giảm sàn
        assertEquals(BigDecimal.valueOf(15000), response.getShopVoucherDiscountTotal()); // Giảm shop

        // Subtotal (200k) + Ship (25k) - Giảm shop (15k) - Giảm sàn (30k) = 180,000 VND
        assertEquals(BigDecimal.valueOf(180000), response.getTotalAmount());

        SubOrderResponse subOrderRes = response.getSubOrders().get(0);
        assertEquals(BigDecimal.valueOf(15000), subOrderRes.getShopVoucherDiscount());
        // SubOrder final = 200k + 25k - 15k = 210,000 VND
        assertEquals(BigDecimal.valueOf(210000), subOrderRes.getFinalAmount());

        // Verify ghi nhận sử dụng cả 2 voucher
        verify(voucherService, times(1)).recordUsage(eq("SAN30K"), eq(userId), eq(1005L), isNull(), eq(BigDecimal.valueOf(30000)));
        verify(voucherService, times(1)).recordUsage(eq("SHOP15K"), eq(userId), eq(1005L), eq(2005L), eq(BigDecimal.valueOf(15000)));
    }

    @Test
    @DisplayName("checkout: Ném BadRequestException khi Voucher không hợp lệ")
    void testCheckout_ThrowsBadRequest_WhenVoucherInvalid() {
        sampleRequest.setVoucherCode("EXPIRED_CODE");

        CatalogVariantValidationDto item = CatalogVariantValidationDto.builder()
                .variantId(101L)
                .productName("Sản phẩm")
                .sku("SKU-1")
                .price(BigDecimal.valueOf(100000))
                .availableStock(10)
                .active(true)
                .shopId(1L)
                .build();

        when(catalogServiceClient.getVariantDetails(101L)).thenReturn(Optional.of(item));

        when(voucherService.validateAndCalculateVoucher(eq("EXPIRED_CODE"), isNull(), any()))
                .thenReturn(VoucherValidationResponse.builder()
                        .valid(false)
                        .message("Voucher đã hết hạn")
                        .build());

        assertThrows(BadRequestException.class, () -> checkoutService.checkout(1L, sampleRequest));
    }

    @Test
    @DisplayName("checkout: Khi request không truyền items, tự động gom toàn bộ mặt hàng từ giỏ hàng Redis")
    void testCheckout_FallbackToRedisCart_Success() {
        Long userId = 5L;
        sampleRequest.setItems(null);

        CartResponse cartResponse = CartResponse.builder()
                .shops(List.of(
                        CartShopGroupResponse.builder()
                                .shopId(8L)
                                .shopName("Shop 8")
                                .items(List.of(
                                        CartItemResponse.builder().variantId(801L).quantity(3).build()
                                ))
                                .build()
                ))
                .build();

        when(cartService.getCart(userId)).thenReturn(cartResponse);

        CatalogVariantValidationDto item = CatalogVariantValidationDto.builder()
                .variantId(801L)
                .productId(88L)
                .productName("Sản phẩm từ giỏ")
                .sku("SKU-CART-801")
                .price(BigDecimal.valueOf(50000))
                .availableStock(10)
                .active(true)
                .shopId(8L)
                .shopName("Shop 8")
                .build();

        when(catalogServiceClient.getVariantDetails(801L)).thenReturn(Optional.of(item));

        when(masterOrderRepository.save(any(MasterOrder.class))).thenAnswer(i -> {
            MasterOrder m = i.getArgument(0);
            m.setId(1005L);
            return m;
        });
        when(subOrderRepository.save(any(SubOrder.class))).thenAnswer(i -> {
            SubOrder s = i.getArgument(0);
            s.setId(2003L);
            return s;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(i -> {
            OrderItem o = i.getArgument(0);
            o.setId(3003L);
            return o;
        });

        CheckoutResponse response = checkoutService.checkout(userId, sampleRequest);

        assertNotNull(response);
        assertEquals(1, response.getTotalShops());
        assertEquals(3, response.getTotalItems());
        assertEquals(BigDecimal.valueOf(150000), response.getSubtotalAmount());
        verify(cartService, times(1)).removeCartItems(eq(userId), eq(List.of(801L)));
    }

    @Test
    @DisplayName("checkout: Ném BadRequestException khi thông tin đầu vào không hợp lệ")
    void testCheckout_ValidationErrors() {
        assertThrows(BadRequestException.class, () -> checkoutService.checkout(null, sampleRequest));
        assertThrows(BadRequestException.class, () -> checkoutService.checkout(-1L, sampleRequest));
        assertThrows(BadRequestException.class, () -> checkoutService.checkout(1L, null));

        sampleRequest.setPaymentMethod("");
        assertThrows(BadRequestException.class, () -> checkoutService.checkout(1L, sampleRequest));

        sampleRequest.setPaymentMethod("COD");
        sampleRequest.setRecipientName("");
        assertThrows(BadRequestException.class, () -> checkoutService.checkout(1L, sampleRequest));

        sampleRequest.setRecipientName("Kien");
        sampleRequest.setRecipientPhone("");
        assertThrows(BadRequestException.class, () -> checkoutService.checkout(1L, sampleRequest));

        sampleRequest.setRecipientPhone("0987");
        sampleRequest.setShippingAddress("");
        assertThrows(BadRequestException.class, () -> checkoutService.checkout(1L, sampleRequest));
    }

    @Test
    @DisplayName("checkout: Ném BadRequestException khi không có mặt hàng nào (giỏ hàng rỗng)")
    void testCheckout_ThrowsBadRequest_WhenEmptyItems() {
        sampleRequest.setItems(List.of());
        when(cartService.getCart(1L)).thenReturn(CartResponse.builder().shops(List.of()).build());

        assertThrows(BadRequestException.class, () -> checkoutService.checkout(1L, sampleRequest));
    }

    @Test
    @DisplayName("checkout: Ném BadRequestException khi biến thể không tồn tại hoặc đã ngừng bán")
    void testCheckout_ThrowsBadRequest_WhenVariantNotFoundOrInactive() {
        when(catalogServiceClient.getVariantDetails(101L)).thenReturn(Optional.empty());
        assertThrows(BadRequestException.class, () -> checkoutService.checkout(1L, sampleRequest));

        CatalogVariantValidationDto inactiveItem = CatalogVariantValidationDto.builder()
                .variantId(101L)
                .productName("Sản phẩm")
                .sku("SKU-1")
                .active(false)
                .build();
        when(catalogServiceClient.getVariantDetails(101L)).thenReturn(Optional.of(inactiveItem));
        assertThrows(BadRequestException.class, () -> checkoutService.checkout(1L, sampleRequest));
    }

    @Test
    @DisplayName("checkout: Ném BadRequestException khi số lượng yêu cầu vượt quá tồn kho")
    void testCheckout_ThrowsBadRequest_WhenInsufficientStock() {
        CatalogVariantValidationDto item = CatalogVariantValidationDto.builder()
                .variantId(101L)
                .productName("Sản phẩm")
                .sku("SKU-1")
                .price(BigDecimal.valueOf(100000))
                .availableStock(1) // Chỉ còn 1, mà request yêu cầu 2
                .active(true)
                .build();

        when(catalogServiceClient.getVariantDetails(101L)).thenReturn(Optional.of(item));

        assertThrows(BadRequestException.class, () -> checkoutService.checkout(1L, sampleRequest));
    }

    @Test
    @DisplayName("previewCheckout: Xem trước chi phí đơn hàng không tạo dữ liệu DB và không xóa Redis cart")
    void testPreviewCheckout_Success() {
        Long userId = 1L;

        CatalogVariantValidationDto item = CatalogVariantValidationDto.builder()
                .variantId(101L)
                .productId(201L)
                .productName("Áo thun")
                .sku("TSHIRT-01")
                .price(BigDecimal.valueOf(100000))
                .availableStock(50)
                .active(true)
                .shopId(99L)
                .shopName("Shop 99")
                .build();

        when(catalogServiceClient.getVariantDetails(101L)).thenReturn(Optional.of(item));

        CheckoutResponse preview = checkoutService.previewCheckout(userId, sampleRequest);

        assertNotNull(preview);
        assertEquals(MasterOrderStatus.DRAFT, preview.getStatus());
        assertEquals(BigDecimal.valueOf(200000), preview.getSubtotalAmount()); // 100k * 2
        assertEquals(BigDecimal.valueOf(25000), preview.getShippingFeeTotal());
        assertEquals(BigDecimal.valueOf(225000), preview.getTotalAmount());
        assertEquals(1, preview.getTotalShops());
        assertEquals(2, preview.getTotalItems());

        // Đảm bảo không ghi DB và không xóa Redis
        verify(masterOrderRepository, never()).save(any());
        verify(subOrderRepository, never()).save(any());
        verify(orderItemRepository, never()).save(any());
        verify(cartService, never()).removeCartItems(any(), any());
    }
}
